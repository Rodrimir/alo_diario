package br.com.cuida.call;

import br.com.cuida.call.vapi.VapiEventParser;
import br.com.cuida.webhook.WebhookEvents;
import br.com.cuida.webhook.WebhookReceived;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

/**
 * Processa os webhooks gravados pelo módulo {@code webhook}. O mock fala o mesmo formato do Vapi,
 * então os dois passam pelo mesmo parser.
 *
 * <p>Sem transação própria: a ligação é atualizada (e o {@link CallEnded} publicado) numa transação
 * e o webhook é marcado em outra, só depois. Falhas viram {@code FAILED} e são reprocessadas.
 */
@Component
class CallWebhookListener {

  static final Set<String> VAPI_FORMAT = Set.of("vapi", "mock");
  static final int MAX_CONFLICT_RETRIES = 3;

  private static final Logger log = LoggerFactory.getLogger(CallWebhookListener.class);

  private final WebhookEvents webhookEvents;
  private final VapiEventParser parser;
  private final CallLifecycle lifecycle;

  CallWebhookListener(
      WebhookEvents webhookEvents, VapiEventParser parser, CallLifecycle lifecycle) {
    this.webhookEvents = webhookEvents;
    this.parser = parser;
    this.lifecycle = lifecycle;
  }

  @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
  void on(WebhookReceived received) {
    if (!VAPI_FORMAT.contains(received.provider())) {
      webhookEvents.markIgnored(received.webhookEventId(), "provedor sem processador");
      return;
    }
    Optional<String> payload = webhookEvents.pendingPayload(received.webhookEventId());
    if (payload.isEmpty()) {
      return;
    }
    try {
      Optional<ProviderCallEvent> event = parser.parse(payload.get());
      if (event.isEmpty()) {
        webhookEvents.markIgnored(received.webhookEventId(), "tipo de evento sem efeito");
        return;
      }
      CallLifecycle.Result result = applyWithRetry(event.get());
      if (result == CallLifecycle.Result.UNKNOWN_CALL) {
        webhookEvents.markIgnored(received.webhookEventId(), "ligação desconhecida");
      } else {
        webhookEvents.markProcessed(received.webhookEventId());
      }
    } catch (RuntimeException e) {
      log.warn(
          "Falha ao processar o webhook {} ({}): {}",
          received.webhookEventId(),
          received.eventType(),
          e.toString());
      webhookEvents.markFailed(
          received.webhookEventId(), e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }

  /**
   * Eventos da mesma ligação podem ser processados em paralelo (ex.: {@code status-update} e {@code
   * end-of-call-report}); quem perde no lock otimista relê a ligação e aplica de novo.
   */
  private CallLifecycle.Result applyWithRetry(ProviderCallEvent event) {
    for (int attempt = 1; ; attempt++) {
      try {
        return lifecycle.apply(event);
      } catch (OptimisticLockingFailureException conflict) {
        if (attempt >= MAX_CONFLICT_RETRIES) {
          throw conflict;
        }
      }
    }
  }
}
