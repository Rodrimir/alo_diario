package br.com.cuida.analise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.ligacao.ConsultaLigacaoService;
import br.com.cuida.ligacao.model.LigacaoDTO;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.OrigemTranscricao;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.StatusLigacao;
import br.com.cuida.ligacao.model.StatusResposta;
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
class PipelineDeAnaliseIT {

  static final Path STORAGE = Path.of("target/test-recordings");

  @Autowired MockMvcTester mvc;

  @Autowired ConsultaLigacaoService consultaLigacaoService;

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

    LigacaoDTO call = callAndWaitProcessed(elderlyId);

    assertThat(call.transcriptSource()).isEqualTo(OrigemTranscricao.PROVIDER);
    assertThat(call.audioObjectKey())
        .isEqualTo("elderly/" + elderlyId + "/calls/" + call.id() + ".mp3");
    assertThat(call.audioMimeType()).isEqualTo("audio/mpeg");
    assertThat(call.audioSizeBytes()).isGreaterThan(1000);
    assertThat(call.providerRecordingUrl()).isNull();
    assertThat(Files.exists(STORAGE.resolve(call.audioObjectKey()))).isTrue();
    assertThat(call.summary().answers()).hasSize(6);
    assertThat(call.summary().answers())
        .extracting(RespostaAtributo::attributeKey)
        .containsExactly(
            "wellbeing",
            "medication",
            "sleep",
            "needed_help",
            "suspicious_call",
            "message_for_family");
    assertThat(call.summary().answers().get(2).answer()).contains("Dormi bem");
    assertThat(call.summary().answers().getFirst().status()).isEqualTo(StatusResposta.ATTENTION);
    assertThat(call.summary().answers().get(4).headline()).isEqualTo("Nenhuma tentativa de golpe");
    assertThat(call.summary().familyMessage())
        .isEqualTo("Diga para minha filha que vou ao médico na segunda-feira.");
    assertThat(call.alertLevel()).isEqualTo(NivelAlerta.ATTENTION);
    assertThat(call.summaryText()).startsWith("Resumo simulado");
  }

  @Test
  void fallReportBecomesUrgent() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987659999");

    LigacaoDTO call = callAndWaitProcessed(elderlyId);

    assertThat(call.alertLevel()).isEqualTo(NivelAlerta.URGENT);
    assertThat(call.summary().alertReasons()).anyMatch(reason -> reason.contains("caí"));
  }

  @Test
  void adminCanReprocessAndTutorCannot() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    LigacaoDTO call = callAndWaitProcessed(elderlyId);

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
                assertThat(consultaLigacaoService.find(call.id()).orElseThrow().status())
                    .isEqualTo(StatusLigacao.PROCESSED));
  }

  @Test
  void deletingTheElderlyDeletesTheRecordings() {
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    LigacaoDTO call = callAndWaitProcessed(elderlyId);
    Path audio = STORAGE.resolve(call.audioObjectKey());
    assertThat(audio).exists();

    assertThat(api.delete(token, "/api/elderly/{id}", elderlyId)).hasStatusOk();

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(audio).doesNotExist());
  }

  private LigacaoDTO callAndWaitProcessed(UUID elderlyId) {
    UUID callId =
        UUID.fromString(
            ApiTestClient.read(
                api.post(token, "/api/elderly/{id}/calls", null, elderlyId), "$.id"));
    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                assertThat(consultaLigacaoService.find(callId).orElseThrow().status())
                    .isEqualTo(StatusLigacao.PROCESSED));
    return consultaLigacaoService.find(callId).orElseThrow();
  }
}
