package br.com.cuida.webhook.model;

import br.com.cuida.webhook.EventoWebhookService;
import java.util.UUID;

/**
 * Publicado (outbox) quando um webhook novo é gravado. Carrega só o id: o payload é lido de {@link
 * EventoWebhookService} por quem processa.
 *
 * @param webhookEventId id em {@code webhook_events}
 * @param provider {@code vapi} ou {@code mock}
 * @param eventType tipo do evento no provedor (ex.: {@code end-of-call-report})
 */
public record WebhookRecebido(UUID webhookEventId, String provider, String eventType) {}
