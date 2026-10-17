package br.com.cuida.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Atalhos para os testes de API: cria usuários e faz chamadas autenticadas. */
public final class ApiTestClient {

  public static final String ADMIN_EMAIL = "admin@teste.local";
  public static final String ADMIN_PASSWORD = "admin-de-teste-123";

  private final MockMvcTester mvc;

  public ApiTestClient(MockMvcTester mvc) {
    this.mvc = mvc;
  }

  /** Registra um tutor novo e devolve o token. */
  public String newTutorToken() {
    String email = "tutor-" + UUID.randomUUID() + "@teste.local";
    MvcTestResult result =
        post(
            null,
            "/api/register",
            """
            {"name": "Tutor de Teste", "email": "%s", "password": "senha-segura-123"}
            """
                .formatted(email));
    assertThat(result).hasStatus(HttpStatus.OK);
    return JsonPath.read(body(result), "$.token");
  }

  public String adminToken() {
    MvcTestResult result =
        post(
            null,
            "/api/login",
            """
            {"email": "%s", "password": "%s"}
            """
                .formatted(ADMIN_EMAIL, ADMIN_PASSWORD));
    assertThat(result).hasStatus(HttpStatus.OK);
    return JsonPath.read(body(result), "$.token");
  }

  public MvcTestResult get(String token, String uri, Object... vars) {
    var request = mvc.get().uri(uri, vars);
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return request.exchange();
  }

  public MvcTestResult post(String token, String uri, String json, Object... vars) {
    var request = mvc.post().uri(uri, vars).contentType(MediaType.APPLICATION_JSON);
    if (json != null) {
      request.content(json);
    }
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return request.exchange();
  }

  public MvcTestResult put(String token, String uri, String json, Object... vars) {
    var request = mvc.put().uri(uri, vars).contentType(MediaType.APPLICATION_JSON).content(json);
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return request.exchange();
  }

  public MvcTestResult delete(String token, String uri, Object... vars) {
    var request = mvc.delete().uri(uri, vars);
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return request.exchange();
  }

  public static String body(MvcTestResult result) {
    return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
  }

  public static <T> T read(MvcTestResult result, String path) {
    return JsonPath.read(body(result), path);
  }
}
