package br.com.cuida.elderly;

import br.com.cuida.shared.time.LocalTimes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** Cadastro do idoso. Datas no fuso do próprio idoso ({@code timezone}). */
record ElderlyResponse(
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

  static ElderlyResponse of(Elderly elderly) {
    ZoneId zone = elderly.getZoneId();
    return new ElderlyResponse(
        elderly.getId(),
        elderly.getTutorId(),
        elderly.getName(),
        elderly.getPreferredName(),
        elderly.getPhoneNumber(),
        zone.getId(),
        elderly.getNotes(),
        elderly.isActive(),
        elderly.hasRecordingConsent(),
        LocalTimes.at(elderly.getConsentRecordedAt(), zone),
        LocalTimes.at(elderly.getCreatedAt(), zone),
        LocalTimes.at(elderly.getUpdatedAt(), zone));
  }
}
