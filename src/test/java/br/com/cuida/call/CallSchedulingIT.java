package br.com.cuida.call;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Agendador, retentativas, disparo e varredura, chamados diretamente com instantes fixos (os jobs
 * ficam desligados no perfil de testes). Datas em 2030 para não interferir nos outros testes.
 */
@IntegrationTest
@AutoConfigureMockMvc
class CallSchedulingIT {

  /** Segunda-feira, 07/01/2030, 09:00 em São Paulo. */
  static final Instant MONDAY_9AM = Instant.parse("2030-01-07T12:00:00Z");

  static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);

  @Autowired MockMvcTester mvc;

  @Autowired CallPlanner planner;

  @Autowired RetryPlanner retryPlanner;

  @Autowired CallDispatcher dispatcher;

  @Autowired StuckCallSweeper sweeper;

  @Autowired CallLifecycle lifecycle;

  @Autowired CallRepository calls;

  @Autowired JdbcTemplate jdbc;

  ApiTestClient api;
  String token;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
    token = api.newTutorToken();
  }

  @Test
  void plansTheFirstAttemptOnceInsideTheWindow() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 3, 20);

    assertThat(ours(planner.planDue(MONDAY_9AM.minusSeconds(60)), elderlyId)).isEmpty();
    List<Call> planned = ours(planner.planDue(MONDAY_9AM.plusSeconds(300)), elderlyId);
    assertThat(ours(planner.planDue(MONDAY_9AM.plusSeconds(360)), elderlyId)).isEmpty();

    assertThat(planned).hasSize(1);
    Call call = planned.getFirst();
    assertThat(call.getStatus()).isEqualTo(CallStatus.SCHEDULED);
    assertThat(call.getTrigger()).isEqualTo(CallTrigger.SCHEDULED);
    assertThat(call.getScheduledFor()).isEqualTo(MONDAY_9AM);
    assertThat(call.getLocalDate()).isEqualTo(MONDAY);
    assertThat(call.getQuestionnaireSnapshot().items()).hasSize(6);
  }

  @Test
  void skipsTheDayAfterTheRecoveryWindow() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 3, 20);

    assertThat(
            ours(planner.planDue(MONDAY_9AM.plus(Duration.ofHours(2)).plusSeconds(60)), elderlyId))
        .isEmpty();
  }

  @Test
  void withoutConsentTheDayIsRecordedAsFailed() {
    UUID elderlyId = elderlyWithSchedule(false, "09:00", 3, 20);

    List<Call> planned = ours(planner.planDue(MONDAY_9AM.plusSeconds(60)), elderlyId);

    assertThat(planned).hasSize(1);
    assertThat(planned.getFirst().getStatus()).isEqualTo(CallStatus.FAILED);
    assertThat(planned.getFirst().getFailureReason())
        .isEqualTo(FailureReasons.RECORDING_CONSENT_MISSING);
  }

  @Test
  void retriesUntilMaxAttempts() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 2, 20);
    Call first = ours(planner.planDue(MONDAY_9AM.plusSeconds(30)), elderlyId).getFirst();
    endAs(first.getId(), "customer-did-not-answer", MONDAY_9AM.plusSeconds(60));

    List<Call> retries = ours(retryPlanner.planRetries(MONDAY_9AM.plusSeconds(120)), elderlyId);
    assertThat(ours(retryPlanner.planRetries(MONDAY_9AM.plusSeconds(180)), elderlyId)).isEmpty();

    assertThat(retries).hasSize(1);
    Call retry = retries.getFirst();
    assertThat(retry.getTrigger()).isEqualTo(CallTrigger.RETRY);
    assertThat(retry.getAttempt()).isEqualTo(2);
    assertThat(retry.getScheduledFor()).isEqualTo(MONDAY_9AM.plusSeconds(60 + 20 * 60));

    endAs(retry.getId(), "customer-busy", MONDAY_9AM.plusSeconds(1300));
    assertThat(ours(retryPlanner.planRetries(MONDAY_9AM.plusSeconds(1400)), elderlyId)).isEmpty();
  }

  @Test
  void dispatchesDueCallsAndFailsTheOnesThatMissedTheWindow() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 3, 20);
    Call call = ours(planner.planDue(MONDAY_9AM.plusSeconds(30)), elderlyId).getFirst();

    dispatcher.dispatch(call.getId(), MONDAY_9AM.plus(Duration.ofHours(3)));

    Call missed = CallTestSupport.reload(calls, call.getId());
    assertThat(missed.getStatus()).isEqualTo(CallStatus.FAILED);
    assertThat(missed.getFailureReason()).isEqualTo(FailureReasons.MISSED_WINDOW);
  }

  @Test
  void dispatchSendsTheCallToTheGateway() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 3, 20);
    Call call = ours(planner.planDue(MONDAY_9AM.plusSeconds(30)), elderlyId).getFirst();

    dispatcher.dispatch(call.getId(), MONDAY_9AM.plusSeconds(40));

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(CallTestSupport.reload(calls, call.getId()).getStatus())
                    .isNotIn(CallStatus.SCHEDULED, CallStatus.DIALING, CallStatus.IN_PROGRESS));
    assertThat(CallTestSupport.reload(calls, call.getId()).getProvider()).isEqualTo("mock");
  }

  @Test
  void stuckCallsFailAndALateReportStillRecoversTheDay() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    Call call = CallTestSupport.dialing(calls, elderlyId, "vapi-" + UUID.randomUUID());
    jdbc.update(
        "update calls set updated_at = now() - interval '2 hours' where id = ?", call.getId());

    sweeper.sweep(Instant.now());

    Call stuck = CallTestSupport.reload(calls, call.getId());
    assertThat(stuck.getStatus()).isEqualTo(CallStatus.FAILED);
    assertThat(stuck.getFailureReason()).isEqualTo(FailureReasons.NO_PROVIDER_REPORT);

    lifecycle.apply(report(call.getId(), "customer-ended-call", Instant.now(), "AI: Oi\nUser: Oi"));

    assertThat(CallTestSupport.reload(calls, call.getId()).getStatus())
        .isEqualTo(CallStatus.COMPLETED);
  }

  private UUID elderlyWithSchedule(boolean consent, String time, int maxAttempts, int interval) {
    UUID elderlyId =
        consent
            ? TestData.createElderlyWithConsent(api, token, "11987654321")
            : TestData.createElderly(api, token, "11987654321");
    assertThat(
            api.put(
                token,
                "/api/elderly/{id}/schedule",
                """
                {"localTime": "%s", "daysOfWeek": ["MONDAY"], "active": true,
                 "maxAttempts": %d, "retryIntervalMinutes": %d}
                """
                    .formatted(time, maxAttempts, interval),
                elderlyId))
        .hasStatusOk();
    return elderlyId;
  }

  private List<Call> ours(List<UUID> ids, UUID elderlyId) {
    return calls.findAllById(ids).stream()
        .filter(call -> call.getElderlyId().equals(elderlyId))
        .toList();
  }

  private void endAs(UUID callId, String endedReason, Instant endedAt) {
    calls.claimForDialing(callId, endedAt.minusSeconds(30));
    lifecycle.apply(report(callId, endedReason, endedAt, null));
  }

  private static ProviderCallEvent.Report report(
      UUID callId, String endedReason, Instant endedAt, String transcript) {
    return new ProviderCallEvent.Report(
        new ProviderCallEvent.CallRef(callId, null),
        endedReason,
        null,
        endedAt,
        0,
        transcript,
        null);
  }
}
