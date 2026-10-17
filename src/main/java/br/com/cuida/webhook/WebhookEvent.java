package br.com.cuida.webhook;

import br.com.cuida.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Evento de webhook recebido de um provedor, gravado <b>antes</b> de ser processado (nada se perde
 * se a aplicação cair no meio). O par {@code (provider, event_id)} é único e garante a
 * idempotência. O {@code payload} é apagado assim que o processamento termina (LGPD: contém
 * transcrição e telefone).
 */
@Entity
@Table(name = "webhook_events")
class WebhookEvent extends BaseEntity {

  static final int ERROR_MAX_LENGTH = 1000;

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WebhookStatus status = WebhookStatus.PENDING;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(length = ERROR_MAX_LENGTH)
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

  void markProcessed(Instant at) {
    this.status = WebhookStatus.PROCESSED;
    this.processedAt = at;
    this.payload = null;
    this.error = null;
    this.attempts++;
  }

  void markIgnored(String reason, Instant at) {
    this.status = WebhookStatus.IGNORED;
    this.processedAt = at;
    this.payload = null;
    this.error = truncate(reason);
    this.attempts++;
  }

  void markFailed(String reason) {
    this.status = WebhookStatus.FAILED;
    this.error = truncate(reason);
    this.attempts++;
  }

  /** Volta para a fila para uma nova tentativa. */
  void requeue() {
    this.status = WebhookStatus.PENDING;
  }

  /** Expurgo de payload de evento que nunca foi processado (retenção). */
  void purgePayload() {
    this.payload = null;
  }

  private static String truncate(String value) {
    if (value == null) {
      return null;
    }
    return value.length() > ERROR_MAX_LENGTH ? value.substring(0, ERROR_MAX_LENGTH) : value;
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

  WebhookStatus getStatus() {
    return status;
  }

  int getAttempts() {
    return attempts;
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
