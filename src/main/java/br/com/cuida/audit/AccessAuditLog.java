package br.com.cuida.audit;

import br.com.cuida.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro imutável de acesso a dado sensível. As referências viram {@code NULL} se o idoso for
 * excluído (LGPD), preservando a trilha de quem acessou e quando.
 */
@Entity
@Table(name = "access_audit_logs")
class AccessAuditLog extends BaseEntity {

  @Column(name = "user_id", updatable = false)
  private UUID userId;

  @Column(name = "elderly_id", updatable = false)
  private UUID elderlyId;

  @Column(name = "call_id", updatable = false)
  private UUID callId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private AuditAction action;

  @Column(length = 45, updatable = false)
  private String ip;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  /** Exigido pelo JPA. */
  protected AccessAuditLog() {}

  AccessAuditLog(
      UUID userId, UUID elderlyId, UUID callId, AuditAction action, String ip, Instant occurredAt) {
    super(UUID.randomUUID());
    this.userId = Objects.requireNonNull(userId, "userId");
    this.elderlyId = elderlyId;
    this.callId = callId;
    this.action = Objects.requireNonNull(action, "action");
    this.ip = ip;
    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
  }

  UUID getUserId() {
    return userId;
  }

  UUID getElderlyId() {
    return elderlyId;
  }

  UUID getCallId() {
    return callId;
  }

  AuditAction getAction() {
    return action;
  }

  String getIp() {
    return ip;
  }

  Instant getOccurredAt() {
    return occurredAt;
  }
}
