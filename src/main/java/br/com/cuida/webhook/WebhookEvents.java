package br.com.cuida.webhook;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** API pública do módulo para quem processa os webhooks: lê o payload e registra o resultado. */
@Service
public class WebhookEvents {

  private final WebhookEventRepository repository;
  private final Clock clock;

  WebhookEvents(WebhookEventRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Payload ainda pendente; vazio se o evento já foi processado/expurgado. */
  @Transactional(readOnly = true)
  public Optional<String> pendingPayload(UUID webhookEventId) {
    return repository
        .findById(webhookEventId)
        .filter(event -> event.getStatus() != WebhookStatus.PROCESSED)
        .filter(event -> event.getStatus() != WebhookStatus.IGNORED)
        .map(WebhookEvent::getPayload);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markProcessed(UUID webhookEventId) {
    repository.findById(webhookEventId).ifPresent(event -> event.markProcessed(clock.instant()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markIgnored(UUID webhookEventId, String reason) {
    repository
        .findById(webhookEventId)
        .ifPresent(event -> event.markIgnored(reason, clock.instant()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID webhookEventId, String reason) {
    repository.findById(webhookEventId).ifPresent(event -> event.markFailed(reason));
  }
}
