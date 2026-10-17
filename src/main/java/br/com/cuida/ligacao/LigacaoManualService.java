package br.com.cuida.ligacao;

import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.infra.exception.ConflitoException;
import br.com.cuida.infra.exception.RegraDeNegocioException;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.LigacaoDTO;
import br.com.cuida.ligacao.model.LigacaoResponseDTO;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.questionario.QuestionarioService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * "Ligar agora": sempre permitido, exceto sem consentimento (422) ou com outra ligação em andamento
 * para o mesmo idoso (409). Não conta para o limite diário nem cancela a ligação agendada.
 */
@Service
class LigacaoManualService {

  private final AcessoIdosoService acessoIdosoService;
  private final QuestionarioService questionarioService;
  private final LigacaoRepository ligacaoRepository;
  private final DisparadorDeLigacoes disparadorDeLigacoes;
  private final TransactionTemplate transactions;
  private final Clock clock;

  LigacaoManualService(
      AcessoIdosoService acessoIdosoService,
      QuestionarioService questionarioService,
      LigacaoRepository ligacaoRepository,
      DisparadorDeLigacoes disparadorDeLigacoes,
      TransactionTemplate transactions,
      Clock clock) {
    this.acessoIdosoService = acessoIdosoService;
    this.questionarioService = questionarioService;
    this.ligacaoRepository = ligacaoRepository;
    this.disparadorDeLigacoes = disparadorDeLigacoes;
    this.transactions = transactions;
    this.clock = clock;
  }

  LigacaoResponseDTO callNow(UUID elderlyId) {
    IdosoDTO elderly = acessoIdosoService.requireAccessible(elderlyId);
    if (!elderly.recordingConsent()) {
      throw new RegraDeNegocioException(
          "Sem consentimento de gravação: registre em PUT /api/elderly/{id}/consent");
    }
    if (ligacaoRepository.existsByElderlyIdAndStatusIn(elderlyId, Ligacao.ACTIVE)) {
      throw new ConflitoException("Já existe uma ligação em andamento para este idoso");
    }
    Instant now = clock.instant();
    LocalDate today = LocalDate.ofInstant(now, elderly.zone());
    Ligacao call =
        transactions.execute(
            status -> {
              int attempt =
                  (int)
                          ligacaoRepository.countByElderlyIdAndLocalDateAndTrigger(
                              elderlyId, today, OrigemLigacao.MANUAL)
                      + 1;
              return ligacaoRepository.saveAndFlush(
                  new Ligacao(
                      elderlyId,
                      OrigemLigacao.MANUAL,
                      now,
                      today,
                      attempt,
                      questionarioService.snapshot(elderlyId)));
            });
    disparadorDeLigacoes.dispatch(call.getId(), now);
    LigacaoDTO view = ligacaoRepository.findById(call.getId()).map(Ligacao::toDTO).orElseThrow();
    return LigacaoResponseDTO.of(view, elderly.zone());
  }
}
