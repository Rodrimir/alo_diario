package br.com.cuida.call;

import br.com.cuida.elderly.ElderlyDirectory;
import br.com.cuida.elderly.ElderlyView;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dispara as ligações vencidas no gateway de voz. Sem transação em volta da chamada externa: a
 * ligação é "reservada" (SCHEDULED → DIALING) antes, para que o job e o disparo manual nunca liguem
 * duas vezes.
 */
@Component
class CallDispatcher {

  private static final Logger log = LoggerFactory.getLogger(CallDispatcher.class);

  private final CallRepository repository;
  private final ElderlyDirectory elderlyDirectory;
  private final ConversationScript script;
  private final VoiceCallGateway gateway;
  private final CallProperties properties;
  private final TransactionTemplate transactions;
  private final Clock clock;

  CallDispatcher(
      CallRepository repository,
      ElderlyDirectory elderlyDirectory,
      ConversationScript script,
      VoiceCallGateway gateway,
      CallProperties properties,
      TransactionTemplate transactions,
      Clock clock) {
    this.repository = repository;
    this.elderlyDirectory = elderlyDirectory;
    this.script = script;
    this.gateway = gateway;
    this.properties = properties;
    this.transactions = transactions;
    this.clock = clock;
  }

  /**
   * @return quantas ligações foram processadas
   */
  int dispatchDue(Instant now) {
    List<UUID> due = repository.findIdsDueForDispatch(now);
    for (UUID callId : due) {
      try {
        dispatch(callId, now);
      } catch (RuntimeException e) {
        log.error("Falha inesperada ao disparar a ligação {}", callId, e);
      }
    }
    return due.size();
  }

  void dispatch(UUID callId, Instant now) {
    Optional<Call> found = repository.findById(callId);
    if (found.isEmpty() || found.get().getStatus() != CallStatus.SCHEDULED) {
      return;
    }
    Call call = found.get();
    if (call.getTrigger() != CallTrigger.MANUAL
        && now.isAfter(call.getScheduledFor().plus(properties.recoveryWindow()))) {
      fail(callId, FailureReasons.MISSED_WINDOW);
      return;
    }
    Integer claimed = transactions.execute(status -> repository.claimForDialing(callId, now));
    if (claimed == null || claimed == 0) {
      return;
    }
    Optional<ElderlyView> elderly = elderlyDirectory.find(call.getElderlyId());
    if (elderly.isEmpty()) {
      return;
    }
    if (!elderly.get().active()) {
      fail(callId, FailureReasons.ELDERLY_INACTIVE);
      return;
    }
    if (!elderly.get().recordingConsent()) {
      fail(callId, FailureReasons.RECORDING_CONSENT_MISSING);
      return;
    }
    StartedCall started;
    try {
      started = gateway.start(script.outboundCall(call, elderly.get()));
    } catch (RuntimeException e) {
      log.warn("Gateway recusou a ligação {}: {}", callId, e.getMessage());
      fail(callId, FailureReasons.withDetail(FailureReasons.DIAL_ERROR, e.getMessage()));
      return;
    }
    transactions.executeWithoutResult(
        status -> repository.recordProvider(callId, gateway.provider(), started.providerCallId()));
    log.info("Ligação {} disparada ({} {})", callId, gateway.provider(), started.providerCallId());
  }

  private void fail(UUID callId, String reason) {
    transactions.executeWithoutResult(
        status ->
            repository
                .findById(callId)
                .ifPresent(
                    call -> {
                      call.fail(reason, clock.instant());
                      repository.saveAndFlush(call);
                    }));
  }
}
