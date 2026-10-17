package br.com.cuida.calendario.model;

import br.com.cuida.auditoria.model.AcaoAuditada;
import java.time.LocalDateTime;
import java.util.UUID;

/** Um acesso a dado sensível do idoso (horário no fuso do idoso). */
public record RegistroAcessoResponseDTO(
    UUID id, UUID userId, UUID callId, AcaoAuditada action, String ip, LocalDateTime occurredAt) {}
