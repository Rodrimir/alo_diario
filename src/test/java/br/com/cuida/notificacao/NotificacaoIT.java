package br.com.cuida.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.notificacao.model.MensagemWhatsAppDTO;
import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Resumo da ligação enviado ao WhatsApp do responsável (whats-api simulado pelo fake). */
@IntegrationTest
@AutoConfigureMockMvc
class NotificacaoIT {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcTemplate jdbc;

  @Autowired FakeWhatsAppService whatsApp;

  @Autowired ReenvioNotificacaoJob reenvioJob;

  ApiTestClient api;
  String token;
  String guardian;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
    token = api.newTutorToken();
    guardian = TestData.randomWhatsapp();
  }

  @AfterEach
  void tearDown() {
    whatsApp.falhar(false);
  }

  @Test
  void processedCallSendsThePitchSummaryToTheGuardian() {
    UUID elderlyId = TestData.createElderlyWithGuardian(api, token, "11987654321", guardian);

    UUID callId = TestData.processedCall(api, token, elderlyId, jdbc);

    await().atMost(Duration.ofSeconds(15)).until(() -> whatsApp.enviadasPara(guardian).size() == 1);
    MensagemWhatsAppDTO mensagem = whatsApp.enviadasPara(guardian).getFirst();
    assertThat(mensagem.template()).isEqualTo("alo_diario_resumo");
    assertThat(mensagem.variaveis())
        .containsEntry("nome", "Dona Maria")
        .containsEntry("recado", "Diga para minha filha que vou ao médico na segunda-feira.")
        .containsEntry("alerta", "⚠️ Há pontos de atenção");
    assertThat(mensagem.variaveis().get("itens"))
        .contains("✅ Tomou os medicamentos")
        .contains(" · ")
        .doesNotContain("\n");

    MvcTestResult list = api.get(token, "/api/calls/{id}/notifications", callId);
    assertThat(list).hasStatusOk();
    assertThat(list).bodyJson().extractingPath("$[0].status").isEqualTo("SENT");
    assertThat(list).bodyJson().extractingPath("$[0].kind").isEqualTo("CALL_SUMMARY");
    assertThat(list).bodyJson().extractingPath("$[0].recipient").isEqualTo(guardian);
    assertThat((String) ApiTestClient.read(list, "$[0].content"))
        .startsWith("ALÔ DIÁRIO – Dona Maria");
    assertThat(api.get(token, "/api/calls/{id}", callId))
        .bodyJson()
        .extractingPath("$.notification.status")
        .isEqualTo("SENT");
  }

  @Test
  void withoutGuardianWhatsappTheNoticeIsSkipped() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    UUID callId = TestData.processedCall(api, token, elderlyId, jdbc);

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(api.get(token, "/api/calls/{id}/notifications", callId))
                    .bodyJson()
                    .extractingPath("$[0].status")
                    .isEqualTo("SKIPPED"));
  }

  @Test
  void reprocessingDoesNotResendButResendDoes() {
    UUID elderlyId = TestData.createElderlyWithGuardian(api, token, "11987654321", guardian);
    UUID callId = TestData.processedCall(api, token, elderlyId, jdbc);
    await().atMost(Duration.ofSeconds(15)).until(() -> whatsApp.enviadasPara(guardian).size() == 1);

    assertThat(api.post(api.adminToken(), "/api/calls/{id}/reprocess", null, callId)).hasStatusOk();
    await()
        .atMost(Duration.ofSeconds(15))
        .until(
            () ->
                "PROCESSED"
                    .equals(
                        jdbc.queryForObject(
                            "select status from calls where id = ?", String.class, callId)));
    MvcTestResult resend = api.post(token, "/api/calls/{id}/notifications/resend", null, callId);

    assertThat(resend).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("SENT");
    assertThat(whatsApp.enviadasPara(guardian)).hasSize(2);
    assertThat(count(callId)).isEqualTo(2);
  }

  @Test
  void failedSendIsRetriedByTheJob() {
    UUID elderlyId = TestData.createElderlyWithGuardian(api, token, "11987654321", guardian);
    UUID callId = TestData.processedCall(api, token, elderlyId, jdbc);
    await().atMost(Duration.ofSeconds(15)).until(() -> whatsApp.enviadasPara(guardian).size() == 1);

    whatsApp.falhar(true);
    assertThat(api.post(token, "/api/calls/{id}/notifications/resend", null, callId))
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("FAILED");
    whatsApp.falhar(false);
    reenvioJob.reenviarFalhas();

    List<String> statuses =
        jdbc.queryForList(
            "select status from notifications where call_id = ? order by created_at",
            String.class,
            callId);
    assertThat(statuses).containsExactly("SENT", "SENT");
  }

  @Test
  void otherTutorCannotSeeNotificationsAndDeletingTheElderlyRemovesThem() {
    UUID elderlyId = TestData.createElderlyWithGuardian(api, token, "11987654321", guardian);
    UUID callId = TestData.processedCall(api, token, elderlyId, jdbc);
    await().atMost(Duration.ofSeconds(15)).until(() -> count(callId) == 1);

    assertThat(api.get(api.newTutorToken(), "/api/calls/{id}/notifications", callId))
        .hasStatus(HttpStatus.NOT_FOUND);
    assertThat(api.delete(token, "/api/elderly/{id}", elderlyId)).hasStatusOk();
    assertThat(count(callId)).isZero();
  }

  private int count(UUID callId) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from notifications where call_id = ?", Integer.class, callId);
    return n == null ? 0 : n;
  }
}
