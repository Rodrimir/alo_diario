package br.com.cuida.idoso.model;

import br.com.cuida.infra.util.HorarioLocal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** Cadastro do idoso. Datas no fuso do próprio idoso ({@code timezone}). */
public record IdosoResponseDTO(
    UUID id,
    UUID tutorId,
    String name,
    String preferredName,
    String phoneNumber,
    String timezone,
    String notes,
    boolean active,
    boolean recordingConsent,
    LocalDateTime consentRecordedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static IdosoResponseDTO of(Idoso elderly) {
    ZoneId zone = elderly.getZoneId();
    return new IdosoResponseDTO(
        elderly.getId(),
        elderly.getTutorId(),
        elderly.getName(),
        elderly.getPreferredName(),
        elderly.getPhoneNumber(),
        zone.getId(),
        elderly.getNotes(),
        elderly.isActive(),
        elderly.hasRecordingConsent(),
        HorarioLocal.at(elderly.getConsentRecordedAt(), zone),
        HorarioLocal.at(elderly.getCreatedAt(), zone),
        HorarioLocal.at(elderly.getUpdatedAt(), zone));
  }
}
