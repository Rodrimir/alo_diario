package br.com.cuida.call;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.questionnaire.AnswerType;
import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class CallPersistenceIT {

  private static final QuestionnaireSnapshot SNAPSHOT =
      new QuestionnaireSnapshot(
          List.of(
              new QuestionnaireSnapshot.Item("sleep", "Dormiu bem?", AnswerType.YES_NO, 0),
              new QuestionnaireSnapshot.Item(
                  "mood", "Como está o humor?", AnswerType.FREE_TEXT, 1)));

  @Autowired CallRepository calls;

  @Autowired JdbcTemplate jdbc;

  @Test
  void roundTripsSnapshotAndStructuredSummaryAsJsonb() {
    UUID elderlyId = newElderly();
    Call call =
        new Call(
            elderlyId,
            CallTrigger.SCHEDULED,
            Instant.parse("2026-10-16T12:00:00Z"),
            LocalDate.of(2026, 10, 16),
            1,
            SNAPSHOT);
    CallSummary summary =
        new CallSummary(
            "Dona Maria dormiu bem e está animada.",
            Mood.GOOD,
            List.of(
                new AttributeAnswer(
                    "sleep", "Dormiu bem?", "Sim, dormiu bem.", AnswerClarity.CLEAR),
                new AttributeAnswer("mood", "Como está o humor?", "", AnswerClarity.NOT_ASKED)),
            List.of("Vai receber a visita da neta."),
            AlertLevel.NONE,
            List.of(),
            false);
    call.recordSummary(summary);
    calls.save(call);

    Call loaded = calls.findById(call.getId()).orElseThrow();
    String mood =
        jdbc.queryForObject(
            "select structured_summary ->> 'mood' from calls where id = ?",
            String.class,
            call.getId());

    assertThat(loaded.getQuestionnaireSnapshot()).isEqualTo(SNAPSHOT);
    assertThat(loaded.getStructuredSummary()).isEqualTo(summary);
    assertThat(loaded.getSummaryText()).isEqualTo(summary.overview());
    assertThat(loaded.getAlertLevel()).isEqualTo(AlertLevel.NONE);
    assertThat(loaded.getStatus()).isEqualTo(CallStatus.SCHEDULED);
    assertThat(loaded.getVersion()).isZero();
    assertThat(mood).isEqualTo("GOOD");
  }

  @Test
  void neverStoresTwoAutomaticAttemptsWithSameNumberOnSameDay() {
    UUID elderlyId = newElderly();
    LocalDate day = LocalDate.of(2026, 10, 16);
    Instant at = Instant.parse("2026-10-16T12:00:00Z");
    calls.saveAndFlush(new Call(elderlyId, CallTrigger.SCHEDULED, at, day, 1, SNAPSHOT));

    assertThatThrownBy(
            () ->
                calls.saveAndFlush(
                    new Call(elderlyId, CallTrigger.SCHEDULED, at, day, 1, SNAPSHOT)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void manualCallsAreOutsideTheDailyUniqueness() {
    UUID elderlyId = newElderly();
    LocalDate day = LocalDate.of(2026, 10, 16);
    Instant at = Instant.parse("2026-10-16T12:00:00Z");
    calls.saveAndFlush(new Call(elderlyId, CallTrigger.SCHEDULED, at, day, 1, SNAPSHOT));
    calls.saveAndFlush(new Call(elderlyId, CallTrigger.MANUAL, at, day, 1, SNAPSHOT));
    calls.saveAndFlush(new Call(elderlyId, CallTrigger.MANUAL, at, day, 1, SNAPSHOT));

    assertThat(
            calls.findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscAttemptAsc(
                elderlyId, day, day))
        .hasSize(3);
  }

  private UUID newElderly() {
    return TestFixtures.insertElderly(jdbc, TestFixtures.insertTutor(jdbc));
  }
}
