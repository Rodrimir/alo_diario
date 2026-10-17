package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Atalhos para montar ligações em estados específicos nos testes do módulo. */
final class LigacaoTestSupport {

  static final QuestionarioSnapshot EMPTY = new QuestionarioSnapshot(List.of());

  private LigacaoTestSupport() {}

  /** Ligação já discada no provedor ({@code DIALING}). */
  static Ligacao dialing(LigacaoRepository calls, UUID elderlyId, String providerCallId) {
    Instant now = Instant.now();
    Ligacao call =
        calls.saveAndFlush(
            new Ligacao(elderlyId, OrigemLigacao.MANUAL, now, LocalDate.now(), 1, EMPTY));
    calls.claimForDialing(call.getId(), now);
    calls.recordProvider(call.getId(), "vapi", providerCallId);
    return calls.findById(call.getId()).orElseThrow();
  }

  static Ligacao reload(LigacaoRepository calls, UUID id) {
    return calls.findById(id).orElseThrow();
  }

  static String status(JdbcTemplate jdbc, UUID callId) {
    return jdbc.queryForObject("select status from calls where id = ?", String.class, callId);
  }
}
