package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.EventoDoProvedor;
import br.com.cuida.ligacao.model.LigacaoEncerrada;
import br.com.cuida.ligacao.vapi.LeitorDeEventosVapi;
import br.com.cuida.webhook.EventoWebhookService;
import br.com.cuida.webhook.model.WebhookRecebido;
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
 * então os dois passam pelo mesmo leitorDeEventosVapi.
 *
 * <p>Sem transação própria: a ligação é atualizada (e o {@link LigacaoEncerrada} publicado) numa
 * transação e o webhook é marcado em outra, só depois. Falhas viram {@code FAILED} e são
 * reprocessadas.
 */
@Component
class WebhookLigacaoListener {

  static final Set<String> VAPI_FORMAT = Set.of("vapi", "mock");
  static final int MAX_CONFLICT_RETRIES = 3;

  private static final Logger log = LoggerFactory.getLogger(WebhookLigacaoListener.class);

  private final EventoWebhookService eventoWebhookService;
  private final LeitorDeEventosVapi leitorDeEventosVapi;
  private final CicloDeVidaDaLigacao cicloDeVidaDaLigacao;

  WebhookLigacaoListener(
      EventoWebhookService eventoWebhookService,
      LeitorDeEventosVapi leitorDeEventosVapi,
      CicloDeVidaDaLigacao cicloDeVidaDaLigacao) {
    this.eventoWebhookService = eventoWebhookService;
    this.leitorDeEventosVapi = leitorDeEventosVapi;
    this.cicloDeVidaDaLigacao = cicloDeVidaDaLigacao;
  }

  @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
  void on(WebhookRecebido received) {
    if (!VAPI_FORMAT.contains(received.provider())) {
      eventoWebhookService.markIgnored(received.webhookEventId(), "provedor sem processador");
      return;
    }
    Optional<String> payload = eventoWebhookService.pendingPayload(received.webhookEventId());
    if (payload.isEmpty()) {
      return;
    }
    try {
      Optional<EventoDoProvedor> event = leitorDeEventosVapi.parse(payload.get());
      if (event.isEmpty()) {
        eventoWebhookService.markIgnored(received.webhookEventId(), "tipo de evento sem efeito");
        return;
      }
      CicloDeVidaDaLigacao.Result result = applyWithRetry(event.get());
      if (result == CicloDeVidaDaLigacao.Result.UNKNOWN_CALL) {
        eventoWebhookService.markIgnored(received.webhookEventId(), "ligação desconhecida");
      } else {
        eventoWebhookService.markProcessed(received.webhookEventId());
      }
    } catch (RuntimeException e) {
      log.warn(
          "Falha ao processar o webhook {} ({}): {}",
          received.webhookEventId(),
          received.eventType(),
          e.toString());
      eventoWebhookService.markFailed(
          received.webhookEventId(), e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }

  /**
   * Eventos da mesma ligação podem ser processados em paralelo (ex.: {@code status-update} e {@code
   * end-of-call-report}); quem perde no lock otimista relê a ligação e aplica de novo.
   */
  private CicloDeVidaDaLigacao.Result applyWithRetry(EventoDoProvedor event) {
    for (int attempt = 1; ; attempt++) {
      try {
        return cicloDeVidaDaLigacao.apply(event);
      } catch (OptimisticLockingFailureException conflict) {
        if (attempt >= MAX_CONFLICT_RETRIES) {
          throw conflict;
        }
      }
    }
  }
}
