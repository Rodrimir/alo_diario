package br.com.cuida.elderly;

import br.com.cuida.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/** Pessoa idosa acompanhada por um tutor. */
@Entity
@Table(name = "elderly")
class Elderly extends AuditableEntity {

  @Column(name = "tutor_id", nullable = false, updatable = false)
  private UUID tutorId;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "preferred_name", length = 60)
  private String preferredName;

  /** Formato E.164 (ex.: {@code +5511987654321}). */
  @Column(name = "phone_number", nullable = false, length = 16)
  private String phoneNumber;

  /** Fuso IANA (ex.: {@code America/Sao_Paulo}). */
  @Column(nullable = false, length = 64)
  private String timezone;

  @Column(length = 2000)
  private String notes;

  @Column(name = "recording_consent", nullable = false)
  private boolean recordingConsent;

  @Column(name = "consent_recorded_at")
  private Instant consentRecordedAt;

  @Column(name = "consent_recorded_by")
  private UUID consentRecordedBy;

  @Column(nullable = false)
  private boolean active = true;

  /** Exigido pelo JPA. */
  protected Elderly() {}

  Elderly(UUID tutorId, String name, String preferredName, String phoneNumber, String timezone) {
    super(UUID.randomUUID());
    this.tutorId = Objects.requireNonNull(tutorId, "tutorId");
    this.name = Objects.requireNonNull(name, "name");
    this.preferredName = preferredName;
    this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
    this.timezone = ZoneId.of(timezone).getId();
  }

  UUID getTutorId() {
    return tutorId;
  }

  String getName() {
    return name;
  }

  String getPreferredName() {
    return preferredName;
  }

  String getPhoneNumber() {
    return phoneNumber;
  }

  ZoneId getZoneId() {
    return ZoneId.of(timezone);
  }

  String getNotes() {
    return notes;
  }

  boolean hasRecordingConsent() {
    return recordingConsent;
  }

  Instant getConsentRecordedAt() {
    return consentRecordedAt;
  }

  UUID getConsentRecordedBy() {
    return consentRecordedBy;
  }

  boolean isActive() {
    return active;
  }
}
