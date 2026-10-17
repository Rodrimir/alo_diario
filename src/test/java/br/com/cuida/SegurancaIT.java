package br.com.cuida;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@AutoConfigureMockMvc
class SegurancaIT {

  @Autowired MockMvcTester mvc;

  ApiTestClient api;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
  }

  @Test
  void corsPreflightAllowsTheConfiguredFrontendOnly() {
    MvcTestResult allowed =
        mvc.options()
            .uri("/api/elderly")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Authorization, Content-Type")
            .exchange();
    MvcTestResult denied =
        mvc.options()
            .uri("/api/elderly")
            .header("Origin", "https://site-estranho.com")
            .header("Access-Control-Request-Method", "POST")
            .exchange();

    assertThat(allowed).hasStatusOk();
    assertThat(allowed.getResponse().getHeader("Access-Control-Allow-Origin"))
        .isEqualTo("http://localhost:3000");
    assertThat(denied).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void pingIsPublic() {
    MvcTestResult result = api.get(null, "/api/ping");

    assertThat(result).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("ok");
  }

  @Test
  void protectedRouteWithoutTokenUsesApiErrorFormat() {
    MvcTestResult result = api.get(null, "/api/elderly");

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(401);
  }

  @Test
  void invalidTokenIsUnauthorized() {
    MvcTestResult result = api.get("nao.e.um.jwt", "/api/elderly");

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void actuatorDetailsAreAdminOnly() {
    assertThat(api.get(api.newTutorToken(), "/actuator/metrics")).hasStatus(HttpStatus.FORBIDDEN);
    assertThat(api.get(api.adminToken(), "/actuator/metrics")).hasStatusOk();
  }

  @Test
  void unknownEndpointIsNotFoundForAuthenticatedUser() {
    MvcTestResult result = api.get(api.newTutorToken(), "/api/nao-existe");

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(404);
  }
}
