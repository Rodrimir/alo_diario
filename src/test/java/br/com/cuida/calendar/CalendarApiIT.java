package br.com.cuida.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Leitura do tutor sobre uma ligação já processada (gateway e IA mock). */
@IntegrationTest
@AutoConfigureMockMvc
class CalendarApiIT {

  static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

  @Autowired MockMvcTester mvc;

  @Autowired JdbcTemplate jdbc;

  ApiTestClient api;
  String token;
  UUID elderlyId;
  UUID callId;

  @BeforeEach
  void setUp() {
    api = new ApiTestClient(mvc);
    token = api.newTutorToken();
    elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    callId = TestData.processedCall(api, token, elderlyId, jdbc);
  }

  @Test
  void calendarShowsTheDayWithItsMainCall() {
    MvcTestResult result =
        api.get(token, "/api/elderly/{id}/calendar?month={m}", elderlyId, YearMonth.now(SAO_PAULO));

    assertThat(result).hasStatusOk();
    assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(1);
    assertThat(result)
        .bodyJson()
        .extractingPath("$[0].date")
        .isEqualTo(LocalDate.now(SAO_PAULO).toString());
    assertThat(result).bodyJson().extractingPath("$[0].callId").isEqualTo(callId.toString());
    assertThat(result).bodyJson().extractingPath("$[0].status").isEqualTo("PROCESSED");
    assertThat(result).bodyJson().extractingPath("$[0].attempts").isEqualTo(1);
    assertThat(result).bodyJson().extractingPath("$[0].hasAudio").isEqualTo(true);
    assertThat(api.get(token, "/api/elderly/{id}/calendar", elderlyId))
        .bodyJson()
        .extractingPath("$.length()")
        .isEqualTo(1);
  }

  @Test
  void invalidMonthIsFieldError() {
    MvcTestResult result = api.get(token, "/api/elderly/{id}/calendar?month=outubro", elderlyId);

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("month");
  }

  @Test
  void dayListsAllAttempts() {
    MvcTestResult result =
        api.get(token, "/api/elderly/{id}/calls?date={d}", elderlyId, LocalDate.now(SAO_PAULO));

    assertThat(result).hasStatusOk();
    assertThat(result).bodyJson().extractingPath("$[0].id").isEqualTo(callId.toString());
    assertThat(result).bodyJson().extractingPath("$[0].trigger").isEqualTo("MANUAL");
  }

  @Test
  void detailTranscriptAndAudioAreServedAndAudited() throws Exception {
    MvcTestResult detail = api.get(token, "/api/calls/{id}", callId);
    assertThat(detail).hasStatusOk();
    assertThat(detail).bodyJson().extractingPath("$.summary.answers.length()").isEqualTo(6);
    assertThat(detail)
        .bodyJson()
        .extractingPath("$.audioUrl")
        .isEqualTo("/api/calls/" + callId + "/audio");

    MvcTestResult transcript = api.get(token, "/api/calls/{id}/transcript", callId);
    assertThat(transcript).hasStatusOk();
    assertThat(transcript).bodyJson().extractingPath("$.source").isEqualTo("PROVIDER");
    assertThat((String) ApiTestClient.read(transcript, "$.transcript"))
        .startsWith("Assistente: Olá, Dona Maria!");

    MvcTestResult audio = api.get(token, "/api/calls/{id}/audio", callId);
    assertThat(audio).hasStatusOk().hasContentType("audio/mpeg");
    assertThat(audio.getResponse().getHeader("Content-Disposition")).startsWith("inline;");
    byte[] expected = new ClassPathResource("mock/sample-call.mp3").getContentAsByteArray();
    assertThat(audio.getResponse().getContentAsByteArray()).isEqualTo(expected);

    List<String> actions =
        jdbc.queryForList(
            "select action from access_audit_logs where call_id = ? order by occurred_at",
            String.class,
            callId);
    assertThat(actions).containsExactly("VIEW_SUMMARY", "VIEW_TRANSCRIPT", "PLAY_AUDIO");
  }

  @Test
  void audioSupportsSingleRangeRequests() {
    MvcTestResult partial =
        mvc.get()
            .uri("/api/calls/{id}/audio", callId)
            .header("Authorization", "Bearer " + token)
            .header("Range", "bytes=0-99")
            .exchange();

    assertThat(partial).hasStatus(HttpStatus.PARTIAL_CONTENT);
    assertThat(partial.getResponse().getContentAsByteArray()).hasSize(100);

    MvcTestResult open =
        mvc.get()
            .uri("/api/calls/{id}/audio", callId)
            .header("Authorization", "Bearer " + token)
            .header("Range", "bytes=0-")
            .exchange();
    assertThat(open).hasStatus(HttpStatus.PARTIAL_CONTENT);
    assertThat(open.getResponse().getContentAsByteArray()).hasSize(23840);
  }

  @Test
  void otherTutorGetsNotFoundForEverything() {
    String other = api.newTutorToken();

    assertThat(api.get(other, "/api/calls/{id}", callId)).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(api.get(other, "/api/calls/{id}/transcript", callId))
        .hasStatus(HttpStatus.NOT_FOUND);
    assertThat(api.get(other, "/api/calls/{id}/audio", callId)).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(api.get(other, "/api/elderly/{id}/calendar", elderlyId))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void purgedAudioIsGone() {
    jdbc.update(
        "update calls set audio_object_key = null, audio_purged_at = now() where id = ?", callId);

    MvcTestResult result = api.get(token, "/api/calls/{id}/audio", callId);

    assertThat(result).hasStatus(HttpStatus.GONE);
    assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(410);
    assertThat(api.get(token, "/api/calls/{id}", callId))
        .bodyJson()
        .extractingPath("$.audioPurged")
        .isEqualTo(true);
  }

  @Test
  void accessLogIsAdminOnly() {
    api.get(token, "/api/calls/{id}/transcript", callId);

    assertThat(api.get(token, "/api/elderly/{id}/access-log", elderlyId))
        .hasStatus(HttpStatus.FORBIDDEN);
    MvcTestResult log = api.get(api.adminToken(), "/api/elderly/{id}/access-log", elderlyId);
    assertThat(log).hasStatusOk();
    assertThat(log).bodyJson().extractingPath("$[0].action").isEqualTo("VIEW_TRANSCRIPT");
    assertThat(log).bodyJson().extractingPath("$[0].callId").isEqualTo(callId.toString());
  }
}
