package br.com.cuida.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class WebhookPayloadRetentionIT {

  @Autowired WebhookEventRepository events;

  @Autowired WebhookPayloadRetentionJob job;

  @Test
  void dropsPayloadOfOldUnfinishedEventsOnly() {
    Instant now = Instant.now();
    WebhookEvent old = failed(now.minus(Duration.ofDays(8)));
    WebhookEvent recent = failed(now.minus(Duration.ofDays(1)));

    job.purge(now);

    assertThat(events.findById(old.getId()).orElseThrow().getPayload()).isNull();
    assertThat(events.findById(old.getId()).orElseThrow().getStatus())
        .isEqualTo(WebhookStatus.FAILED);
    assertThat(events.findById(recent.getId()).orElseThrow().getPayload()).isNotNull();
  }

  private WebhookEvent failed(Instant receivedAt) {
    WebhookEvent event =
        new WebhookEvent(
            "vapi", "evt-" + UUID.randomUUID(), "end-of-call-report", "{}", receivedAt);
    event.markFailed("erro");
    return events.saveAndFlush(event);
  }
}
