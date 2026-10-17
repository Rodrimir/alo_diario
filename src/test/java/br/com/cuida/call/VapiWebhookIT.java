package br.com.cuida.call;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** {@code POST /webhook/vapi}: autenticação por segredo, idempotência e efeito na ligação. */
@IntegrationTest
@AutoConfigureMockMvc
class VapiWebhookIT {

  static final String SECRET = "segredo-do-webhook-de-teste";

  @Autowired MockMvcTester mvc;

  @Autowired CallRepository calls;

  @Autowired JdbcTemplate jdbc;

  ApiTestClient api;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
  }

  @Test
  void rejectsMissingOrWrongSecret() {
    String body = statusUpdate("vapi-x");

    assertThat(post(null, body)).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(post("errado", body)).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void appliesStatusAndReportIdempotently() {
    String providerCallId = "vapi-" + UUID.randomUUID();
    UUID elderlyId = TestData.createElderlyWithConsent(api, api.newTutorToken(), "11987654321");
    Call call = CallTestSupport.dialing(calls, elderlyId, providerCallId);

    MvcTestResult status = post(SECRET, statusUpdate(providerCallId));
    assertThat(status).hasStatusOk();
    assertThat(ApiTestClient.body(status)).isEqualTo("EVENT_RECEIVED");
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> assertThat(CallTestSupport.status(jdbc, call.getId())).isEqualTo("IN_PROGRESS"));

    String report = busyReport(providerCallId);
    assertThat(post(SECRET, report)).hasStatusOk();
    assertThat(post(SECRET, report)).hasStatusOk();

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> assertThat(CallTestSupport.status(jdbc, call.getId())).isEqualTo("BUSY"));
    Integer stored =
        jdbc.queryForObject(
            "select count(*) from webhook_events where provider = 'vapi' and event_id = ?",
            Integer.class,
            providerCallId + ":end-of-call-report");
    assertThat(stored).isEqualTo(1);
  }

  @Test
  void unknownCallIsIgnoredAndPayloadDiscarded() {
    String providerCallId = "vapi-desconhecida-" + UUID.randomUUID();

    assertThat(post(SECRET, busyReport(providerCallId))).hasStatusOk();

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForMap(
                            "select status, payload from webhook_events where event_id = ?",
                            providerCallId + ":end-of-call-report"))
                    .containsEntry("status", "IGNORED")
                    .containsEntry("payload", null));
  }

  @Test
  void otherMessageTypesAreAcknowledgedButNotStored() {
    String providerCallId = "vapi-" + UUID.randomUUID();

    assertThat(
            post(
                SECRET,
                """
                {"message": {"type": "speech-update", "call": {"id": "%s"}}}
                """
                    .formatted(providerCallId)))
        .hasStatusOk();

    Integer stored =
        jdbc.queryForObject(
            "select count(*) from webhook_events where event_id like ?",
            Integer.class,
            providerCallId + "%");
    assertThat(stored).isZero();
  }

  private MvcTestResult post(String secret, String body) {
    var request =
        mvc.post().uri("/webhook/vapi").contentType(MediaType.APPLICATION_JSON).content(body);
    if (secret != null) {
      request.header("X-Webhook-Secret", secret);
    }
    return request.exchange();
  }

  private static String statusUpdate(String providerCallId) {
    return """
        {"message": {"type": "status-update", "status": "in-progress",
                     "timestamp": 1792238400000, "call": {"id": "%s"}}}
        """
        .formatted(providerCallId);
  }

  private static String busyReport(String providerCallId) {
    return """
        {"message": {"type": "end-of-call-report", "endedReason": "customer-busy",
                     "durationSeconds": 0, "call": {"id": "%s"}}}
        """
        .formatted(providerCallId);
  }
}
