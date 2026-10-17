package br.com.cuida.call;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** "Ligar agora" de ponta a ponta com o gateway mock (webhooks simulados no formato do Vapi). */
@IntegrationTest
@AutoConfigureMockMvc
class CallFlowIT {

  @Autowired MockMvcTester mvc;

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
  void manualCallIsDialedAndEndsThroughTheWebhookFlow() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");

    MvcTestResult result = api.post(token, "/api/elderly/{id}/calls", null, elderlyId);

    assertThat(result).hasStatusOk();
    assertThat(result).bodyJson().extractingPath("$.trigger").isEqualTo("MANUAL");
    assertThat(result).bodyJson().extractingPath("$.attempt").isEqualTo(1);
    UUID callId = UUID.fromString(ApiTestClient.read(result, "$.id"));

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(CallTestSupport.status(jdbc, callId))
                    .isIn("COMPLETED", "PROCESSING", "PROCESSED", "PROCESSING_FAILED"));
    Call call = CallTestSupport.reload(calls, callId);
    assertThat(call.getProvider()).isEqualTo("mock");
    assertThat(call.getProviderCallId()).startsWith("mock-");
    assertThat(call.getDurationSeconds()).isEqualTo(95);
    assertThat(call.getEndReason()).isEqualTo("assistant-ended-call");
    assertThat(call.getTranscript()).startsWith("Assistente: Olá, Dona Maria!");
    assertThat(call.getTranscript()).contains("Idoso(a): ");
    assertThat(call.getQuestionnaireSnapshot().items()).hasSize(6);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<Map<String, Object>> events =
                  jdbc.queryForList(
                      "select status, payload from webhook_events where event_id like ?",
                      call.getProviderCallId() + "%");
              assertThat(events).hasSize(2);
              assertThat(events)
                  .allSatisfy(
                      e -> {
                        assertThat(e.get("status")).isEqualTo("PROCESSED");
                        assertThat(e.get("payload")).isNull();
                      });
            });
  }

  @Test
  void noAnswerScenarioEndsAsNoAnswer() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987650000");

    UUID callId =
        UUID.fromString(
            ApiTestClient.read(
                api.post(token, "/api/elderly/{id}/calls", null, elderlyId), "$.id"));

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> assertThat(CallTestSupport.status(jdbc, callId)).isEqualTo("NO_ANSWER"));
  }

  @Test
  void dialErrorIsReportedImmediately() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987652222");

    MvcTestResult result = api.post(token, "/api/elderly/{id}/calls", null, elderlyId);

    assertThat(result).hasStatusOk();
    assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("FAILED");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.failureReason")
        .isEqualTo("DIAL_ERROR: falha simulada ao discar");
  }

  @Test
  void withoutConsentIsUnprocessable() {
    UUID elderlyId = TestData.createElderly(api, token, "11987654321");

    MvcTestResult result = api.post(token, "/api/elderly/{id}/calls", null, elderlyId);

    assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(422);
  }

  @Test
  void callInProgressIsConflict() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    CallTestSupport.dialing(calls, elderlyId, "vapi-" + UUID.randomUUID());

    MvcTestResult result = api.post(token, "/api/elderly/{id}/calls", null, elderlyId);

    assertThat(result).hasStatus(HttpStatus.CONFLICT);
  }

  @Test
  void otherTutorCannotCall() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");

    assertThat(api.post(api.newTutorToken(), "/api/elderly/{id}/calls", null, elderlyId))
        .hasStatus(HttpStatus.NOT_FOUND);
  }
}
