package br.com.cuida.schedule;

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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@AutoConfigureMockMvc
class ScheduleApiIT {

  @Autowired MockMvcTester mvc;

  ApiTestClient api;
  String token;
  UUID elderlyId;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
    token = api.newTutorToken();
    elderlyId = TestData.createElderly(api, token, "11987654321");
  }

  @Test
  void activatesScheduleAndComputesNextCall() {
    MvcTestResult result =
        api.put(
            token,
            "/api/elderly/{id}/schedule",
            """
            {"localTime": "10:15", "daysOfWeek": ["FRIDAY", "MONDAY"], "active": true,
             "maxAttempts": 2, "retryIntervalMinutes": 30}
            """,
            elderlyId);

    assertThat(result).hasStatusOk();
    assertThat(result).bodyJson().extractingPath("$.localTime").isEqualTo("10:15");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.daysOfWeek")
        .asArray()
        .containsExactly("MONDAY", "FRIDAY");
    assertThat(result).bodyJson().extractingPath("$.maxAttempts").isEqualTo(2);
    assertThat(result).bodyJson().extractingPath("$.nextCallAt").isNotNull();
  }

  @Test
  void defaultsAttemptsAndInterval() {
    MvcTestResult result =
        api.put(
            token,
            "/api/elderly/{id}/schedule",
            """
            {"localTime": "08:00", "daysOfWeek": ["MONDAY"], "active": false}
            """,
            elderlyId);

    assertThat(result).hasStatusOk();
    assertThat(result).bodyJson().extractingPath("$.maxAttempts").isEqualTo(3);
    assertThat(result).bodyJson().extractingPath("$.retryIntervalMinutes").isEqualTo(20);
  }

  @Test
  void activeScheduleNeedsAtLeastOneDay() {
    MvcTestResult result =
        api.put(
            token,
            "/api/elderly/{id}/schedule",
            """
            {"localTime": "08:00", "daysOfWeek": [], "active": true}
            """,
            elderlyId);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("daysOfWeek");
  }

  @Test
  void rejectsOutOfRangeAttempts() {
    MvcTestResult result =
        api.put(
            token,
            "/api/elderly/{id}/schedule",
            """
            {"localTime": "08:00", "daysOfWeek": ["MONDAY"], "active": true, "maxAttempts": 9}
            """,
            elderlyId);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("maxAttempts");
  }

  @Test
  void otherTutorCannotSeeSchedule() {
    assertThat(api.get(api.newTutorToken(), "/api/elderly/{id}/schedule", elderlyId))
        .hasStatus(HttpStatus.NOT_FOUND);
  }
}
