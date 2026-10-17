package br.com.cuida.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.springframework.http.HttpStatus;
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

  public static UUID createElderlyWithConsent(ApiTestClient api, String token, String phone) {
    UUID id = createElderly(api, token, phone);
    assertThat(api.put(token, "/api/elderly/{id}/consent", "{\"recordingConsent\": true}", id))
        .hasStatus(HttpStatus.OK);
    return id;
  }
}
