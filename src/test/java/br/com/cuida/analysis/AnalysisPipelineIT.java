package br.com.cuida.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.call.AlertLevel;
import br.com.cuida.call.CallQueries;
import br.com.cuida.call.CallStatus;
import br.com.cuida.call.CallView;
import br.com.cuida.call.TranscriptSource;
import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Ligação mock → webhook → gravação copiada → resumo (mock) → PROCESSED. */
@IntegrationTest
@AutoConfigureMockMvc
class AnalysisPipelineIT {

  static final Path STORAGE = Path.of("target/test-recordings");

  @Autowired MockMvcTester mvc;

  @Autowired CallQueries calls;

  ApiTestClient api;
  String token;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
    token = api.newTutorToken();
  }

  @Test
  void completedCallIsProcessedWithAudioAndSummary() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");

    CallView call = callAndWaitProcessed(elderlyId);

    assertThat(call.transcriptSource()).isEqualTo(TranscriptSource.PROVIDER);
    assertThat(call.audioObjectKey())
        .isEqualTo("elderly/" + elderlyId + "/calls/" + call.id() + ".mp3");
    assertThat(call.audioMimeType()).isEqualTo("audio/mpeg");
    assertThat(call.audioSizeBytes()).isGreaterThan(1000);
    assertThat(call.providerRecordingUrl()).isNull();
    assertThat(Files.exists(STORAGE.resolve(call.audioObjectKey()))).isTrue();
    assertThat(call.summary().answers()).hasSize(6);
    assertThat(call.summary().answers().getFirst().answer()).contains("Dormi bem");
    assertThat(call.alertLevel()).isEqualTo(AlertLevel.ATTENTION);
    assertThat(call.summaryText()).startsWith("Resumo simulado");
  }

  @Test
  void fallReportBecomesUrgent() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987659999");

    CallView call = callAndWaitProcessed(elderlyId);

    assertThat(call.alertLevel()).isEqualTo(AlertLevel.URGENT);
    assertThat(call.summary().alertReasons()).anyMatch(reason -> reason.contains("caí"));
  }

  @Test
  void adminCanReprocessAndTutorCannot() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    CallView call = callAndWaitProcessed(elderlyId);

    assertThat(api.post(token, "/api/calls/{id}/reprocess", null, call.id()))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(api.post(api.adminToken(), "/api/calls/{id}/reprocess", null, call.id()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("COMPLETED");

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(calls.find(call.id()).orElseThrow().status())
                    .isEqualTo(CallStatus.PROCESSED));
  }

  @Test
  void deletingTheElderlyDeletesTheRecordings() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    CallView call = callAndWaitProcessed(elderlyId);
    Path audio = STORAGE.resolve(call.audioObjectKey());
    assertThat(audio).exists();

    assertThat(api.delete(token, "/api/elderly/{id}", elderlyId)).hasStatusOk();

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(audio).doesNotExist());
  }

  private CallView callAndWaitProcessed(UUID elderlyId) {
    UUID callId =
        UUID.fromString(
            ApiTestClient.read(
                api.post(token, "/api/elderly/{id}/calls", null, elderlyId), "$.id"));
    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                assertThat(calls.find(callId).orElseThrow().status())
                    .isEqualTo(CallStatus.PROCESSED));
    return calls.find(callId).orElseThrow();
  }
}
