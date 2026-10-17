package br.com.cuida.ligacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.ligacao.model.EventoDoProvedor;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.MotivosDeFalha;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.ligacao.model.StatusLigacao;
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
class AgendamentoDeLigacoesIT {

  /** Segunda-feira, 07/01/2030, 09:00 em São Paulo. */
  static final Instant MONDAY_9AM = Instant.parse("2030-01-07T12:00:00Z");

  static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);

  @Autowired MockMvcTester mvc;

  @Autowired PlanejadorDeLigacoes planejadorDeLigacoes;

  @Autowired PlanejadorDeRetentativas planejadorDeRetentativas;

  @Autowired DisparadorDeLigacoes disparadorDeLigacoes;

  @Autowired VarredorDeLigacoesTravadas varredorDeLigacoesTravadas;

  @Autowired CicloDeVidaDaLigacao cicloDeVidaDaLigacao;

  @Autowired LigacaoRepository ligacaoRepository;

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

    assertThat(ours(planejadorDeLigacoes.planDue(MONDAY_9AM.minusSeconds(60)), elderlyId))
        .isEmpty();
    List<Ligacao> planned =
        ours(planejadorDeLigacoes.planDue(MONDAY_9AM.plusSeconds(300)), elderlyId);
    assertThat(ours(planejadorDeLigacoes.planDue(MONDAY_9AM.plusSeconds(360)), elderlyId))
        .isEmpty();

    assertThat(planned).hasSize(1);
    Ligacao call = planned.getFirst();
    assertThat(call.getStatus()).isEqualTo(StatusLigacao.SCHEDULED);
    assertThat(call.getTrigger()).isEqualTo(OrigemLigacao.SCHEDULED);
    assertThat(call.getScheduledFor()).isEqualTo(MONDAY_9AM);
    assertThat(call.getLocalDate()).isEqualTo(MONDAY);
    assertThat(call.getQuestionnaireSnapshot().items()).hasSize(6);
  }

  @Test
  void skipsTheDayAfterTheRecoveryWindow() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 3, 20);

    assertThat(
            ours(
                planejadorDeLigacoes.planDue(MONDAY_9AM.plus(Duration.ofHours(2)).plusSeconds(60)),
                elderlyId))
        .isEmpty();
  }

  @Test
  void withoutConsentTheDayIsRecordedAsFailed() {
    UUID elderlyId = elderlyWithSchedule(false, "09:00", 3, 20);

    List<Ligacao> planned =
        ours(planejadorDeLigacoes.planDue(MONDAY_9AM.plusSeconds(60)), elderlyId);

    assertThat(planned).hasSize(1);
    assertThat(planned.getFirst().getStatus()).isEqualTo(StatusLigacao.FAILED);
    assertThat(planned.getFirst().getFailureReason())
        .isEqualTo(MotivosDeFalha.RECORDING_CONSENT_MISSING);
  }

  @Test
  void retriesUntilMaxAttempts() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 2, 20);
    Ligacao first =
        ours(planejadorDeLigacoes.planDue(MONDAY_9AM.plusSeconds(30)), elderlyId).getFirst();
    endAs(first.getId(), "customer-did-not-answer", MONDAY_9AM.plusSeconds(60));

    List<Ligacao> retries =
        ours(planejadorDeRetentativas.planRetries(MONDAY_9AM.plusSeconds(120)), elderlyId);
    assertThat(ours(planejadorDeRetentativas.planRetries(MONDAY_9AM.plusSeconds(180)), elderlyId))
        .isEmpty();

    assertThat(retries).hasSize(1);
    Ligacao retry = retries.getFirst();
    assertThat(retry.getTrigger()).isEqualTo(OrigemLigacao.RETRY);
    assertThat(retry.getAttempt()).isEqualTo(2);
    assertThat(retry.getScheduledFor()).isEqualTo(MONDAY_9AM.plusSeconds(60 + 20 * 60));

    endAs(retry.getId(), "customer-busy", MONDAY_9AM.plusSeconds(1300));
    assertThat(ours(planejadorDeRetentativas.planRetries(MONDAY_9AM.plusSeconds(1400)), elderlyId))
        .isEmpty();
  }

  @Test
  void dispatchesDueCallsAndFailsTheOnesThatMissedTheWindow() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 3, 20);
    Ligacao call =
        ours(planejadorDeLigacoes.planDue(MONDAY_9AM.plusSeconds(30)), elderlyId).getFirst();

    disparadorDeLigacoes.dispatch(call.getId(), MONDAY_9AM.plus(Duration.ofHours(3)));

    Ligacao missed = LigacaoTestSupport.reload(ligacaoRepository, call.getId());
    assertThat(missed.getStatus()).isEqualTo(StatusLigacao.FAILED);
    assertThat(missed.getFailureReason()).isEqualTo(MotivosDeFalha.MISSED_WINDOW);
  }

  @Test
  void dispatchSendsTheCallToTheGateway() {
    UUID elderlyId = elderlyWithSchedule(true, "09:00", 3, 20);
    Ligacao call =
        ours(planejadorDeLigacoes.planDue(MONDAY_9AM.plusSeconds(30)), elderlyId).getFirst();

    disparadorDeLigacoes.dispatch(call.getId(), MONDAY_9AM.plusSeconds(40));

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(LigacaoTestSupport.reload(ligacaoRepository, call.getId()).getStatus())
                    .isNotIn(
                        StatusLigacao.SCHEDULED, StatusLigacao.DIALING, StatusLigacao.IN_PROGRESS));
    assertThat(LigacaoTestSupport.reload(ligacaoRepository, call.getId()).getProvider())
        .isEqualTo("mock");
  }

  @Test
  void stuckCallsFailAndALateReportStillRecoversTheDay() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    Ligacao call =
        LigacaoTestSupport.dialing(ligacaoRepository, elderlyId, "vapi-" + UUID.randomUUID());
    jdbc.update(
        "update calls set updated_at = now() - interval '2 hours' where id = ?", call.getId());

    varredorDeLigacoesTravadas.sweep(Instant.now());

    Ligacao stuck = LigacaoTestSupport.reload(ligacaoRepository, call.getId());
    assertThat(stuck.getStatus()).isEqualTo(StatusLigacao.FAILED);
    assertThat(stuck.getFailureReason()).isEqualTo(MotivosDeFalha.NO_PROVIDER_REPORT);

    cicloDeVidaDaLigacao.apply(
        report(call.getId(), "customer-ended-call", Instant.now(), "AI: Oi\nUser: Oi"));

    assertThat(LigacaoTestSupport.reload(ligacaoRepository, call.getId()).getStatus())
        .isEqualTo(StatusLigacao.COMPLETED);
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

  private List<Ligacao> ours(List<UUID> ids, UUID elderlyId) {
    return ligacaoRepository.findAllById(ids).stream()
        .filter(call -> call.getElderlyId().equals(elderlyId))
        .toList();
  }

  private void endAs(UUID callId, String endedReason, Instant endedAt) {
    ligacaoRepository.claimForDialing(callId, endedAt.minusSeconds(30));
    cicloDeVidaDaLigacao.apply(report(callId, endedReason, endedAt, null));
  }

  private static EventoDoProvedor.Report report(
      UUID callId, String endedReason, Instant endedAt, String transcript) {
    return new EventoDoProvedor.Report(
        new EventoDoProvedor.CallRef(callId, null),
        endedReason,
        null,
        endedAt,
        0,
        transcript,
        null);
  }
}
