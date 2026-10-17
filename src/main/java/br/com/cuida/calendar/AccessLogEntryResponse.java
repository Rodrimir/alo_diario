package br.com.cuida.calendar;

import br.com.cuida.audit.AuditAction;
import java.time.LocalDateTime;
import java.util.UUID;

/** Um acesso a dado sensível do idoso (horário no fuso do idoso). */
record AccessLogEntryResponse(
    UUID id, UUID userId, UUID callId, AuditAction action, String ip, LocalDateTime occurredAt) {}
