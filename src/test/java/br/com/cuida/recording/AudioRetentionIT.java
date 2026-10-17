package br.com.cuida.recording;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.ApiTestClient;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestData;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
@AutoConfigureMockMvc
class AudioRetentionIT {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcTemplate jdbc;

  @Autowired AudioRetentionJob job;

  @Test
  void purgesAudioOlderThanRetentionAndKeepsTheRest() {
    ApiTestClient api = new ApiTestClient(mvc);
    String token = api.newTutorToken();
    UUID elderlyId = TestData.createElderlyWithConsent(api, token, "11987654321");
    UUID oldCall = TestData.processedCall(api, token, elderlyId, jdbc);
    jdbc.update("update calls set ended_at = now() - interval '91 days' where id = ?", oldCall);
    String key =
        jdbc.queryForObject(
            "select audio_object_key from calls where id = ?", String.class, oldCall);

    assertThat(job.purge(Instant.now())).isGreaterThanOrEqualTo(1);

    Map<String, Object> row =
        jdbc.queryForMap(
            "select audio_object_key, audio_purged_at, transcript from calls where id = ?",
            oldCall);
    assertThat(row.get("audio_object_key")).isNull();
    assertThat(row.get("audio_purged_at")).isNotNull();
    assertThat(row.get("transcript")).isNotNull();
    assertThat(Path.of("target/test-recordings").resolve(key)).doesNotExist();
    assertThat(api.get(token, "/api/calls/{id}/audio", oldCall)).hasStatus(HttpStatus.GONE);
  }
}
