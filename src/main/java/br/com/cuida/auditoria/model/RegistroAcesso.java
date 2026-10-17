package br.com.cuida.auditoria.model;

import br.com.cuida.infra.persistencia.EntidadeBase;
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
public class RegistroAcesso extends EntidadeBase {

  @Column(name = "user_id", updatable = false)
  private UUID userId;

  @Column(name = "elderly_id", updatable = false)
  private UUID elderlyId;

  @Column(name = "call_id", updatable = false)
  private UUID callId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private AcaoAuditada action;

  @Column(length = 45, updatable = false)
  private String ip;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  /** Exigido pelo JPA. */
  protected RegistroAcesso() {}

  public RegistroAcesso(
      UUID userId,
      UUID elderlyId,
      UUID callId,
      AcaoAuditada action,
      String ip,
      Instant occurredAt) {
    super(UUID.randomUUID());
    this.userId = Objects.requireNonNull(userId, "userId");
    this.elderlyId = elderlyId;
    this.callId = callId;
    this.action = Objects.requireNonNull(action, "action");
    this.ip = ip;
    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getElderlyId() {
    return elderlyId;
  }

  public UUID getCallId() {
    return callId;
  }

  public AcaoAuditada getAction() {
    return action;
  }

  public String getIp() {
    return ip;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
