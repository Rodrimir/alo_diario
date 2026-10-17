package br.com.cuida.webhook;

import java.util.UUID;

/**
 * Publicado (outbox) quando um webhook novo é gravado. Carrega só o id: o payload é lido de {@link
 * WebhookEvents} por quem processa.
 *
 * @param webhookEventId id em {@code webhook_events}
 * @param provider {@code vapi} ou {@code mock}
 * @param eventType tipo do evento no provedor (ex.: {@code end-of-call-report})
 */
public record WebhookReceived(UUID webhookEventId, String provider, String eventType) {}
