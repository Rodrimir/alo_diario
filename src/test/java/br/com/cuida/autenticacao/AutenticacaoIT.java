package br.com.cuida.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@AutoConfigureMockMvc
class AutenticacaoIT {

  @Autowired MockMvcTester mvc;

  ApiTestClient api;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
  }

  @Test
  void registerReturnsTokenAndTutorRoleThenLoginWorks() {
    String email = "Nova." + UUID.randomUUID() + "@Teste.Local";
    MvcTestResult register =
        api.post(
            null,
            "/api/register",
            """
            {"name": "Ana Souza", "email": "%s", "password": "senha-segura-123"}
            """
                .formatted(email));

    assertThat(register).hasStatus(HttpStatus.OK);
    assertThat(register).bodyJson().extractingPath("$.role").isEqualTo("TUTOR");
    assertThat(register).bodyJson().extractingPath("$.email").isEqualTo(email.toLowerCase());
    assertThat(register).bodyJson().extractingPath("$.expiresIn").isEqualTo(3600);

    MvcTestResult login =
        api.post(
            null,
            "/api/login",
            """
            {"email": "%s", "password": "senha-segura-123"}
            """
                .formatted(email));
    assertThat(login).hasStatus(HttpStatus.OK);
    assertThat(login).bodyJson().extractingPath("$.name").isEqualTo("Ana Souza");
    assertThat((String) ApiTestClient.read(login, "$.token")).isNotBlank();
  }

  @Test
  void duplicateEmailIsConflict() {
    String email = "dup-" + UUID.randomUUID() + "@teste.local";
    String body =
        """
        {"name": "Ana", "email": "%s", "password": "senha-segura-123"}
        """
            .formatted(email);
    api.post(null, "/api/register", body);

    MvcTestResult again = api.post(null, "/api/register", body);

    assertThat(again).hasStatus(HttpStatus.CONFLICT);
    assertThat(again).bodyJson().extractingPath("$.status").isEqualTo(409);
    assertThat(again).bodyJson().extractingPath("$.message").isEqualTo("E-mail já cadastrado");
  }

  @Test
  void wrongPasswordIsUnauthorized() {
    MvcTestResult result =
        api.post(
            null,
            "/api/login",
            """
            {"email": "%s", "password": "errada-123"}
            """
                .formatted(ApiTestClient.ADMIN_EMAIL));

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.message")
        .isEqualTo("E-mail ou senha inválidos");
  }

  @Test
  void validationErrorsListEachField() {
    MvcTestResult result =
        api.post(
            null,
            "/api/register",
            """
            {"name": "", "email": "não-é-email", "password": "123"}
            """);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(400);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.errors[*].field")
        .asArray()
        .containsExactlyInAnyOrder("name", "email", "password");
  }

  @Test
  void bootstrapAdminCanLogIn() {
    String token = api.adminToken();

    assertThat(token).isNotBlank();
  }
}
