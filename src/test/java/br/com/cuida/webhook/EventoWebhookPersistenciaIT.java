package br.com.cuida.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.webhook.model.EventoWebhook;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class EventoWebhookPersistenciaIT {

  @Autowired EventoWebhookRepository eventoWebhookRepository;

  @Autowired JdbcTemplate jdbc;

  @Test
  void storesRawPayloadAsJsonb() {
    String eventId = "call-" + UUID.randomUUID() + ":end-of-call-report";
    String payload = "{\"message\":{\"type\":\"end-of-call-report\",\"endedReason\":\"hangup\"}}";
    eventoWebhookRepository.save(
        new EventoWebhook("vapi", eventId, "end-of-call-report", payload, Instant.now()));

    EventoWebhook loaded =
        eventoWebhookRepository.findByProviderAndEventId("vapi", eventId).orElseThrow();
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
    eventoWebhookRepository.saveAndFlush(
        new EventoWebhook("mock", eventId, "x", "{}", Instant.now()));

    assertThatThrownBy(
            () ->
                eventoWebhookRepository.saveAndFlush(
                    new EventoWebhook("mock", eventId, "x", "{}", Instant.now())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
