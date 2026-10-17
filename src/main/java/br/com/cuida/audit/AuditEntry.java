package br.com.cuida.audit;

import java.time.Instant;
import java.util.UUID;

/** Registro de acesso a dado sensível (API pública do módulo). */
public record AuditEntry(
    UUID id,
    UUID userId,
    UUID elderlyId,
    UUID callId,
    AuditAction action,
    String ip,
    Instant occurredAt) {}
