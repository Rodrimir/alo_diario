package br.com.cuida.auditoria.model;

import java.time.Instant;
import java.util.UUID;

/** Registro de acesso a dado sensível (API pública do módulo). */
public record RegistroAcessoDTO(
    UUID id,
    UUID userId,
    UUID elderlyId,
    UUID callId,
    AcaoAuditada action,
    String ip,
    Instant occurredAt) {}
