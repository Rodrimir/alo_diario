package br.com.cuida.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.testsupport.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class WebhookRetryIT {

  @Autowired WebhookEventRepository events;

  @Autowired WebhookRetryJob retryJob;

  @Test
  void failedEventsAreRequeuedAndProcessedAgain() {
    String payload =
        """
        {"message": {"type": "end-of-call-report", "endedReason": "customer-busy",
                     "call": {"id": "mock-%s"}}}
        """
            .formatted(UUID.randomUUID());
    WebhookEvent event =
        new WebhookEvent(
            "mock", "evt-" + UUID.randomUUID(), "end-of-call-report", payload, Instant.now());
    event.markFailed("falha simulada");
    events.saveAndFlush(event);

    assertThat(retryJob.retryFailed()).isGreaterThanOrEqualTo(1);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              WebhookEvent reloaded = events.findById(event.getId()).orElseThrow();
              assertThat(reloaded.getStatus()).isEqualTo(WebhookStatus.IGNORED);
              assertThat(reloaded.getAttempts()).isEqualTo(2);
              assertThat(reloaded.getPayload()).isNull();
            });
  }
}
