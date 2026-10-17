package br.com.cuida.webhook;

import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Porta de entrada dos webhooks (API pública do módulo): grava o evento e publica {@link
 * WebhookReceived} na mesma transação. Um evento repetido (mesmo provedor e id) é reconhecido e
 * descartado, inclusive quando duas entregas chegam ao mesmo tempo.
 */
@Service
public class WebhookInbox {

  /** Resultado do recebimento. */
  public enum Result {
    ACCEPTED,
    DUPLICATE
  }

  private final WebhookEventRepository repository;
  private final ApplicationEventPublisher events;
  private final TransactionTemplate transactions;
  private final Clock clock;

  WebhookInbox(
      WebhookEventRepository repository,
      ApplicationEventPublisher events,
      TransactionTemplate transactions,
      Clock clock) {
    this.repository = repository;
    this.events = events;
    this.transactions = transactions;
    this.clock = clock;
  }

  public Result receive(String provider, String eventId, String eventType, String payload) {
    if (repository.existsByProviderAndEventId(provider, eventId)) {
      return Result.DUPLICATE;
    }
    try {
      transactions.executeWithoutResult(
          status -> {
            WebhookEvent event =
                repository.saveAndFlush(
                    new WebhookEvent(provider, eventId, eventType, payload, clock.instant()));
            events.publishEvent(new WebhookReceived(event.getId(), provider, eventType));
          });
      return Result.ACCEPTED;
    } catch (DataIntegrityViolationException concurrentDuplicate) {
      return Result.DUPLICATE;
    }
  }
}
