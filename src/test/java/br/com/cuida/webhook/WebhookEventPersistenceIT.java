package br.com.cuida.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.testsupport.IntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class WebhookEventPersistenceIT {

  @Autowired WebhookEventRepository events;

  @Autowired JdbcTemplate jdbc;

  @Test
  void storesRawPayloadAsJsonb() {
    String eventId = "call-" + UUID.randomUUID() + ":end-of-call-report";
    String payload = "{\"message\":{\"type\":\"end-of-call-report\",\"endedReason\":\"hangup\"}}";
    events.save(new WebhookEvent("vapi", eventId, "end-of-call-report", payload, Instant.now()));

    WebhookEvent loaded = events.findByProviderAndEventId("vapi", eventId).orElseThrow();
    String type =
        jdbc.queryForObject(
            "select payload -> 'message' ->> 'type' from webhook_events where event_id = ?",
            String.class,
            eventId);

    assertThat(type).isEqualTo("end-of-call-report");
    assertThat(loaded.getPayload()).contains("\"endedReason\"");
    assertThat(loaded.getProcessedAt()).isNull();
  }

  @Test
  void providerAndEventIdAreUnique() {
    String eventId = "evt-" + UUID.randomUUID();
    events.saveAndFlush(new WebhookEvent("mock", eventId, "x", "{}", Instant.now()));

    assertThatThrownBy(
            () -> events.saveAndFlush(new WebhookEvent("mock", eventId, "x", "{}", Instant.now())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
