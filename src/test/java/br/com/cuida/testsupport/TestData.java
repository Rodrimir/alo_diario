package br.com.cuida.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Cadastros prontos pela API, para os testes de fluxo. */
public final class TestData {

  private TestData() {}

  /** Cadastra um idoso para o tutor do token e devolve o id. */
  public static UUID createElderly(ApiTestClient api, String token, String phone) {
    MvcTestResult result =
        api.post(
            token,
            "/api/elderly",
            """
            {"name": "Maria da Silva", "preferredName": "Dona Maria",
             "phoneNumber": "%s", "timezone": "America/Sao_Paulo"}
            """
                .formatted(phone));
    assertThat(result).hasStatus(HttpStatus.OK);
    return UUID.fromString(ApiTestClient.read(result, "$.id"));
  }

  /** Dispara "ligar agora" (gateway mock) e espera o pipeline terminar. @return id da ligação */
  public static UUID processedCall(
      ApiTestClient api, String token, UUID elderlyId, JdbcTemplate jdbc) {
    MvcTestResult result = api.post(token, "/api/elderly/{id}/calls", null, elderlyId);
    assertThat(result).hasStatus(HttpStatus.OK);
    UUID callId = UUID.fromString(ApiTestClient.read(result, "$.id"));
    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "select status from calls where id = ?", String.class, callId))
                    .isEqualTo("PROCESSED"));
    return callId;
  }

  /** Idoso com consentimento e com o WhatsApp do responsável (quem recebe o resumo). */
  public static UUID createElderlyWithGuardian(
      ApiTestClient api, String token, String phone, String guardianWhatsapp) {
    MvcTestResult result =
        api.post(
            token,
            "/api/elderly",
            """
            {"name": "Maria da Silva", "preferredName": "Dona Maria", "phoneNumber": "%s",
             "guardianName": "Ana", "guardianWhatsapp": "%s"}
            """
                .formatted(phone, guardianWhatsapp));
    assertThat(result).hasStatus(HttpStatus.OK);
    UUID id = UUID.fromString(ApiTestClient.read(result, "$.id"));
    assertThat(api.put(token, "/api/elderly/{id}/consent", "{\"recordingConsent\": true}", id))
        .hasStatus(HttpStatus.OK);
    return id;
  }

  /** Um WhatsApp brasileiro aleatório em E.164, para isolar os testes que usam o fake. */
  public static String randomWhatsapp() {
    return "+55119" + String.format("%08d", new java.util.Random().nextInt(100_000_000));
  }

  public static UUID createElderlyWithConsent(ApiTestClient api, String token, String phone) {
    UUID id = createElderly(api, token, phone);
    assertThat(api.put(token, "/api/elderly/{id}/consent", "{\"recordingConsent\": true}", id))
        .hasStatus(HttpStatus.OK);
    return id;
  }
}
