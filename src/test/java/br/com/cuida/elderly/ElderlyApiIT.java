package br.com.cuida.elderly;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@AutoConfigureMockMvc
class ElderlyApiIT {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcTemplate jdbc;

  ApiTestClient api;
  String token;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
    token = api.newTutorToken();
  }

  @Test
  void createNormalizesPhoneAndCreatesDefaultsInactive() {
    MvcTestResult created =
        api.post(
            token,
            "/api/elderly",
            """
            {"name": "José Pereira", "preferredName": "Seu Zé", "phoneNumber": "(21) 99876-5432",
             "timezone": "America/Recife", "notes": "Usa aparelho auditivo"}
            """);

    assertThat(created).hasStatus(HttpStatus.OK);
    assertThat(created).bodyJson().extractingPath("$.phoneNumber").isEqualTo("+5521998765432");
    assertThat(created).bodyJson().extractingPath("$.timezone").isEqualTo("America/Recife");
    assertThat(created).bodyJson().extractingPath("$.recordingConsent").isEqualTo(false);
    assertThat(created).bodyJson().extractingPath("$.active").isEqualTo(true);
    UUID id = UUID.fromString(ApiTestClient.read(created, "$.id"));

    assertThat(api.get(token, "/api/elderly/{id}/questionnaire", id))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.length()")
        .isEqualTo(6);
    MvcTestResult schedule = api.get(token, "/api/elderly/{id}/schedule", id);
    assertThat(schedule).hasStatusOk().bodyJson().extractingPath("$.active").isEqualTo(false);
    assertThat(schedule).bodyJson().extractingPath("$.localTime").isEqualTo("09:00");
    assertThat(schedule).bodyJson().extractingPath("$.nextCallAt").isNull();
  }

  @Test
  void invalidPhoneAndTimezoneAreFieldErrors() {
    MvcTestResult result =
        api.post(
            token,
            "/api/elderly",
            """
            {"name": "José", "phoneNumber": "123", "timezone": "America/Atlantida"}
            """);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("phoneNumber");
  }

  @Test
  void tutorOnlySeesOwnElderlyAndOtherTutorGets404() {
    UUID mine = TestData.createElderly(api, token, "11987654321");
    String otherToken = api.newTutorToken();

    assertThat(api.get(token, "/api/elderly"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].id")
        .asArray()
        .containsExactly(mine.toString());
    MvcTestResult foreign = api.get(otherToken, "/api/elderly/{id}", mine);
    assertThat(foreign).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(foreign)
        .bodyJson()
        .extractingPath("$.message")
        .isEqualTo("Idoso não encontrado: " + mine);
    assertThat(api.get(otherToken, "/api/elderly"))
        .bodyJson()
        .extractingPath("$.length()")
        .isEqualTo(0);
  }

  @Test
  void adminSeesEveryElderly() {
    UUID mine = TestData.createElderly(api, token, "11987654321");

    assertThat(api.get(api.adminToken(), "/api/elderly/{id}", mine)).hasStatusOk();
  }

  @Test
  void updateReplacesTheRegistration() {
    UUID id = TestData.createElderly(api, token, "11987654321");

    MvcTestResult updated =
        api.put(
            token,
            "/api/elderly/{id}",
            """
            {"name": "Maria Souza", "phoneNumber": "+55 11 91234-5678", "active": false}
            """,
            id);

    assertThat(updated).hasStatusOk();
    assertThat(updated).bodyJson().extractingPath("$.name").isEqualTo("Maria Souza");
    assertThat(updated).bodyJson().extractingPath("$.preferredName").isNull();
    assertThat(updated).bodyJson().extractingPath("$.active").isEqualTo(false);
    assertThat(updated).bodyJson().extractingPath("$.timezone").isEqualTo("America/Sao_Paulo");
  }

  @Test
  void consentRecordsWhoAndWhen() {
    UUID id = TestData.createElderly(api, token, "11987654321");

    MvcTestResult result =
        api.put(token, "/api/elderly/{id}/consent", "{\"recordingConsent\": true}", id);

    assertThat(result).hasStatusOk();
    assertThat(result).bodyJson().extractingPath("$.recordingConsent").isEqualTo(true);
    assertThat(result).bodyJson().extractingPath("$.consentRecordedAt").isNotNull();
    UUID recordedBy =
        jdbc.queryForObject("select consent_recorded_by from elderly where id = ?", UUID.class, id);
    assertThat(recordedBy).isNotNull();
  }

  @Test
  void deleteRemovesElderlyAndCascades() {
    UUID id = TestData.createElderly(api, token, "11987654321");

    assertThat(api.delete(token, "/api/elderly/{id}", id)).hasStatus(HttpStatus.OK);

    assertThat(api.get(token, "/api/elderly/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
    Integer leftovers =
        jdbc.queryForObject(
            "select (select count(*) from questionnaire_items where elderly_id = ?)"
                + " + (select count(*) from call_schedules where elderly_id = ?)",
            Integer.class,
            id,
            id);
    assertThat(leftovers).isZero();
  }
}
