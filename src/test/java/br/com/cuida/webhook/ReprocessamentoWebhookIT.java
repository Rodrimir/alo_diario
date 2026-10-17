package br.com.cuida.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.webhook.model.EventoWebhook;
import br.com.cuida.webhook.model.StatusWebhook;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ReprocessamentoWebhookIT {

  @Autowired EventoWebhookRepository eventoWebhookRepository;

  @Autowired ReprocessamentoWebhookJob reprocessamentoWebhookJob;

  @Test
  void failedEventsAreRequeuedAndProcessedAgain() {
    String payload =
        """
        {"message": {"type": "end-of-call-report", "endedReason": "customer-busy",
                     "call": {"id": "mock-%s"}}}
        """
            .formatted(UUID.randomUUID());
    EventoWebhook event =
        new EventoWebhook(
            "mock", "evt-" + UUID.randomUUID(), "end-of-call-report", payload, Instant.now());
    event.markFailed("falha simulada");
    eventoWebhookRepository.saveAndFlush(event);

    assertThat(reprocessamentoWebhookJob.retryFailed()).isGreaterThanOrEqualTo(1);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              EventoWebhook reloaded =
                  eventoWebhookRepository.findById(event.getId()).orElseThrow();
              assertThat(reloaded.getStatus()).isEqualTo(StatusWebhook.IGNORED);
              assertThat(reloaded.getAttempts()).isEqualTo(2);
              assertThat(reloaded.getPayload()).isNull();
            });
  }
}
