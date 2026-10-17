package br.com.cuida.call;

import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Atalhos para montar ligações em estados específicos nos testes do módulo. */
final class CallTestSupport {

  static final QuestionnaireSnapshot EMPTY = new QuestionnaireSnapshot(List.of());

  private CallTestSupport() {}

  /** Ligação já discada no provedor ({@code DIALING}). */
  static Call dialing(CallRepository calls, UUID elderlyId, String providerCallId) {
    Instant now = Instant.now();
    Call call =
        calls.saveAndFlush(new Call(elderlyId, CallTrigger.MANUAL, now, LocalDate.now(), 1, EMPTY));
    calls.claimForDialing(call.getId(), now);
    calls.recordProvider(call.getId(), "vapi", providerCallId);
    return calls.findById(call.getId()).orElseThrow();
  }

  static Call reload(CallRepository calls, UUID id) {
    return calls.findById(id).orElseThrow();
  }

  static String status(JdbcTemplate jdbc, UUID callId) {
    return jdbc.queryForObject("select status from calls where id = ?", String.class, callId);
  }
}
