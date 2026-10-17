package br.com.cuida.call;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ligações em andamento sem relatório final do provedor depois de {@code app.calls.stuck-after} são
 * marcadas {@code FAILED / NO_PROVIDER_REPORT}. Não geram retentativa (a pessoa pode ter atendido),
 * e um relatório atrasado ainda recupera o dia.
 */
@Component
class StuckCallSweeper {

  private static final Logger log = LoggerFactory.getLogger(StuckCallSweeper.class);

  private final CallRepository repository;
  private final CallProperties properties;
  private final TransactionTemplate transactions;

  StuckCallSweeper(
      CallRepository repository, CallProperties properties, TransactionTemplate transactions) {
    this.repository = repository;
    this.properties = properties;
    this.transactions = transactions;
  }

  /**
   * @return quantas ligações foram encerradas
   */
  int sweep(Instant now) {
    List<UUID> stuck = repository.findIdsActiveSince(now.minus(properties.stuckAfter()));
    for (UUID callId : stuck) {
      transactions.executeWithoutResult(
          status ->
              repository
                  .findById(callId)
                  .filter(Call::isActive)
                  .ifPresent(
                      call -> {
                        call.fail(FailureReasons.NO_PROVIDER_REPORT, now);
                        repository.saveAndFlush(call);
                        log.warn("Ligação {} sem relatório do provedor: marcada FAILED", callId);
                      }));
    }
    return stuck.size();
  }
}
