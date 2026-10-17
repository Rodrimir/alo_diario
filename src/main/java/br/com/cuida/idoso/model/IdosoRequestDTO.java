package br.com.cuida.idoso.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /api/elderly} e {@code PUT /api/elderly/{id}} (substitui o cadastro todo).
 *
 * @param phoneNumber qualquer formato; é normalizado para E.164 ({@code +5511987654321})
 * @param timezone fuso IANA; padrão {@code America/Sao_Paulo}
 * @param active se {@code false}, o agendador não liga; padrão {@code true}
 * @param guardianName nome do responsável que recebe o resumo
 * @param guardianWhatsapp WhatsApp do responsável, qualquer formato (normalizado para E.164)
 */
public record IdosoRequestDTO(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 60) String preferredName,
    @NotBlank @Size(max = 30) String phoneNumber,
    @Size(max = 64) String timezone,
    @Size(max = 2000) String notes,
    Boolean active,
    @Size(max = 120) String guardianName,
    @Size(max = 30) String guardianWhatsapp) {}
