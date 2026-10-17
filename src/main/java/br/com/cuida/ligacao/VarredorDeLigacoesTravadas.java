package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.MotivosDeFalha;
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
class VarredorDeLigacoesTravadas {

  private static final Logger log = LoggerFactory.getLogger(VarredorDeLigacoesTravadas.class);

  private final LigacaoRepository ligacaoRepository;
  private final LigacaoProperties ligacaoProperties;
  private final TransactionTemplate transactions;

  VarredorDeLigacoesTravadas(
      LigacaoRepository ligacaoRepository,
      LigacaoProperties ligacaoProperties,
      TransactionTemplate transactions) {
    this.ligacaoRepository = ligacaoRepository;
    this.ligacaoProperties = ligacaoProperties;
    this.transactions = transactions;
  }

  /**
   * @return quantas ligações foram encerradas
   */
  int sweep(Instant now) {
    List<UUID> stuck =
        ligacaoRepository.findIdsActiveSince(now.minus(ligacaoProperties.stuckAfter()));
    for (UUID callId : stuck) {
      transactions.executeWithoutResult(
          status ->
              ligacaoRepository
                  .findById(callId)
                  .filter(Ligacao::isActive)
                  .ifPresent(
                      call -> {
                        call.fail(MotivosDeFalha.NO_PROVIDER_REPORT, now);
                        ligacaoRepository.saveAndFlush(call);
                        log.warn("Ligação {} sem relatório do provedor: marcada FAILED", callId);
                      }));
    }
    return stuck.size();
  }
}
