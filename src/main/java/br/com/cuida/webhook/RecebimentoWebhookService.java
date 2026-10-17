package br.com.cuida.webhook;

import br.com.cuida.webhook.model.EventoWebhook;
import br.com.cuida.webhook.model.WebhookRecebido;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Porta de entrada dos webhooks (API pública do módulo): grava o evento e publica {@link
 * WebhookRecebido} na mesma transação. Um evento repetido (mesmo provedor e id) é reconhecido e
 * descartado, inclusive quando duas entregas chegam ao mesmo tempo.
 */
@Service
public class RecebimentoWebhookService {

  /** Resultado do recebimento. */
  public enum Result {
    ACCEPTED,
    DUPLICATE
  }

  private final EventoWebhookRepository eventoWebhookRepository;
  private final ApplicationEventPublisher events;
  private final TransactionTemplate transactions;
  private final Clock clock;

  RecebimentoWebhookService(
      EventoWebhookRepository eventoWebhookRepository,
      ApplicationEventPublisher events,
      TransactionTemplate transactions,
      Clock clock) {
    this.eventoWebhookRepository = eventoWebhookRepository;
    this.events = events;
    this.transactions = transactions;
    this.clock = clock;
  }

  public Result receive(String provider, String eventId, String eventType, String payload) {
    if (eventoWebhookRepository.existsByProviderAndEventId(provider, eventId)) {
      return Result.DUPLICATE;
    }
    try {
      transactions.executeWithoutResult(
          status -> {
            EventoWebhook event =
                eventoWebhookRepository.saveAndFlush(
                    new EventoWebhook(provider, eventId, eventType, payload, clock.instant()));
            events.publishEvent(new WebhookRecebido(event.getId(), provider, eventType));
          });
      return Result.ACCEPTED;
    } catch (DataIntegrityViolationException concurrentDuplicate) {
      return Result.DUPLICATE;
    }
  }
}
