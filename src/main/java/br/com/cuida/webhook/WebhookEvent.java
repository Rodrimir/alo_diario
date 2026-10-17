package br.com.cuida.webhook;

import br.com.cuida.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Evento de webhook recebido de um provedor. O par {@code (provider, event_id)} é único e garante a
 * idempotência. O {@code payload} é apagado após o processamento bem-sucedido (LGPD).
 */
@Entity
@Table(name = "webhook_events")
class WebhookEvent extends BaseEntity {

  @Column(nullable = false, updatable = false, length = 20)
  private String provider;

  @Column(name = "event_id", nullable = false, updatable = false, length = 200)
  private String eventId;

  @Column(name = "event_type", length = 60)
  private String eventType;

  /** JSON bruto recebido (texto). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String payload;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(length = 1000)
  private String error;

  /** Exigido pelo JPA. */
  protected WebhookEvent() {}

  WebhookEvent(
      String provider, String eventId, String eventType, String payload, Instant receivedAt) {
    super(UUID.randomUUID());
    this.provider = Objects.requireNonNull(provider, "provider");
    this.eventId = Objects.requireNonNull(eventId, "eventId");
    this.eventType = eventType;
    this.payload = payload;
    this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
  }

  String getProvider() {
    return provider;
  }

  String getEventId() {
    return eventId;
  }

  String getEventType() {
    return eventType;
  }

  String getPayload() {
    return payload;
  }

  Instant getReceivedAt() {
    return receivedAt;
  }

  Instant getProcessedAt() {
    return processedAt;
  }

  String getError() {
    return error;
  }
}
