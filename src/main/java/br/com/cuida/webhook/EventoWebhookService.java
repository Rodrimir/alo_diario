package br.com.cuida.webhook;

import br.com.cuida.webhook.model.EventoWebhook;
import br.com.cuida.webhook.model.StatusWebhook;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** API pública do módulo para quem processa os webhooks: lê o payload e registra o resultado. */
@Service
public class EventoWebhookService {

  private final EventoWebhookRepository eventoWebhookRepository;
  private final Clock clock;

  EventoWebhookService(EventoWebhookRepository eventoWebhookRepository, Clock clock) {
    this.eventoWebhookRepository = eventoWebhookRepository;
    this.clock = clock;
  }

  /** Payload ainda pendente; vazio se o evento já foi processado/expurgado. */
  @Transactional(readOnly = true)
  public Optional<String> pendingPayload(UUID webhookEventId) {
    return eventoWebhookRepository
        .findById(webhookEventId)
        .filter(event -> event.getStatus() != StatusWebhook.PROCESSED)
        .filter(event -> event.getStatus() != StatusWebhook.IGNORED)
        .map(EventoWebhook::getPayload);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markProcessed(UUID webhookEventId) {
    eventoWebhookRepository
        .findById(webhookEventId)
        .ifPresent(event -> event.markProcessed(clock.instant()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markIgnored(UUID webhookEventId, String reason) {
    eventoWebhookRepository
        .findById(webhookEventId)
        .ifPresent(event -> event.markIgnored(reason, clock.instant()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID webhookEventId, String reason) {
    eventoWebhookRepository.findById(webhookEventId).ifPresent(event -> event.markFailed(reason));
  }
}
