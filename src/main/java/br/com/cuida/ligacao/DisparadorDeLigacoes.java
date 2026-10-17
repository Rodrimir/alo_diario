package br.com.cuida.ligacao;

import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.LigacaoIniciada;
import br.com.cuida.ligacao.model.MotivosDeFalha;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.ligacao.model.StatusLigacao;
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
 * Dispara as ligações vencidas no gatewayDeVoz de voz. Sem transação em volta da chamada externa: a
 * ligação é "reservada" (SCHEDULED → DIALING) antes, para que o job e o disparo manual nunca liguem
 * duas vezes.
 */
@Component
class DisparadorDeLigacoes {

  private static final Logger log = LoggerFactory.getLogger(DisparadorDeLigacoes.class);

  private final LigacaoRepository ligacaoRepository;
  private final AcessoIdosoService acessoIdosoService;
  private final RoteiroDaConversa roteiroDaConversa;
  private final GatewayDeVoz gatewayDeVoz;
  private final LigacaoProperties ligacaoProperties;
  private final TransactionTemplate transactions;
  private final Clock clock;

  DisparadorDeLigacoes(
      LigacaoRepository ligacaoRepository,
      AcessoIdosoService acessoIdosoService,
      RoteiroDaConversa roteiroDaConversa,
      GatewayDeVoz gatewayDeVoz,
      LigacaoProperties ligacaoProperties,
      TransactionTemplate transactions,
      Clock clock) {
    this.ligacaoRepository = ligacaoRepository;
    this.acessoIdosoService = acessoIdosoService;
    this.roteiroDaConversa = roteiroDaConversa;
    this.gatewayDeVoz = gatewayDeVoz;
    this.ligacaoProperties = ligacaoProperties;
    this.transactions = transactions;
    this.clock = clock;
  }

  /**
   * @return quantas ligações foram processadas
   */
  int dispatchDue(Instant now) {
    List<UUID> due = ligacaoRepository.findIdsDueForDispatch(now);
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
    Optional<Ligacao> found = ligacaoRepository.findById(callId);
    if (found.isEmpty() || found.get().getStatus() != StatusLigacao.SCHEDULED) {
      return;
    }
    Ligacao call = found.get();
    if (call.getTrigger() != OrigemLigacao.MANUAL
        && now.isAfter(call.getScheduledFor().plus(ligacaoProperties.recoveryWindow()))) {
      fail(callId, MotivosDeFalha.MISSED_WINDOW);
      return;
    }
    Integer claimed =
        transactions.execute(status -> ligacaoRepository.claimForDialing(callId, now));
    if (claimed == null || claimed == 0) {
      return;
    }
    Optional<IdosoDTO> elderly = acessoIdosoService.find(call.getElderlyId());
    if (elderly.isEmpty()) {
      return;
    }
    if (!elderly.get().active()) {
      fail(callId, MotivosDeFalha.ELDERLY_INACTIVE);
      return;
    }
    if (!elderly.get().recordingConsent()) {
      fail(callId, MotivosDeFalha.RECORDING_CONSENT_MISSING);
      return;
    }
    LigacaoIniciada started;
    try {
      started = gatewayDeVoz.start(roteiroDaConversa.outboundCall(call, elderly.get()));
    } catch (RuntimeException e) {
      log.warn("Gateway recusou a ligação {}: {}", callId, e.getMessage());
      fail(callId, MotivosDeFalha.withDetail(MotivosDeFalha.DIAL_ERROR, e.getMessage()));
      return;
    }
    transactions.executeWithoutResult(
        status ->
            ligacaoRepository.recordProvider(
                callId, gatewayDeVoz.provider(), started.providerCallId()));
    log.info(
        "Ligação {} disparada ({} {})", callId, gatewayDeVoz.provider(), started.providerCallId());
  }

  private void fail(UUID callId, String reason) {
    transactions.executeWithoutResult(
        status ->
            ligacaoRepository
                .findById(callId)
                .ifPresent(
                    call -> {
                      call.fail(reason, clock.instant());
                      ligacaoRepository.saveAndFlush(call);
                    }));
  }
}
