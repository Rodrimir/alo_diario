package br.com.cuida.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.webhook.model.EventoWebhook;
import br.com.cuida.webhook.model.StatusWebhook;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class RetencaoPayloadWebhookIT {

  @Autowired EventoWebhookRepository eventoWebhookRepository;

  @Autowired RetencaoPayloadWebhookJob retencaoPayloadWebhookJob;

  @Test
  void dropsPayloadOfOldUnfinishedEventsOnly() {
    Instant now = Instant.now();
    EventoWebhook old = failed(now.minus(Duration.ofDays(8)));
    EventoWebhook recent = failed(now.minus(Duration.ofDays(1)));

    retencaoPayloadWebhookJob.purge(now);

    assertThat(eventoWebhookRepository.findById(old.getId()).orElseThrow().getPayload()).isNull();
    assertThat(eventoWebhookRepository.findById(old.getId()).orElseThrow().getStatus())
        .isEqualTo(StatusWebhook.FAILED);
    assertThat(eventoWebhookRepository.findById(recent.getId()).orElseThrow().getPayload())
        .isNotNull();
  }

  private EventoWebhook failed(Instant receivedAt) {
    EventoWebhook event =
        new EventoWebhook(
            "vapi", "evt-" + UUID.randomUUID(), "end-of-call-report", "{}", receivedAt);
    event.markFailed("erro");
    return eventoWebhookRepository.saveAndFlush(event);
  }
}
