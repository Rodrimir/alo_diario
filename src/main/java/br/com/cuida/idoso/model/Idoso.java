package br.com.cuida.idoso.model;

import br.com.cuida.infra.persistencia.EntidadeAuditavel;
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
public class Idoso extends EntidadeAuditavel {

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

  /** Quem recebe o resumo diário (pode ser diferente do tutor). */
  @Column(name = "guardian_name", length = 120)
  private String guardianName;

  /** WhatsApp do responsável em E.164; sem ele, o resumo não é enviado. */
  @Column(name = "guardian_whatsapp", length = 16)
  private String guardianWhatsapp;

  /** Exigido pelo JPA. */
  protected Idoso() {}

  public Idoso(
      UUID tutorId, String name, String preferredName, String phoneNumber, String timezone) {
    super(UUID.randomUUID());
    this.tutorId = Objects.requireNonNull(tutorId, "tutorId");
    update(name, preferredName, phoneNumber, timezone, null, true);
  }

  public void update(
      String name,
      String preferredName,
      String phoneNumber,
      String timezone,
      String notes,
      boolean active) {
    this.name = Objects.requireNonNull(name, "name").trim();
    this.preferredName = blankToNull(preferredName);
    this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
    this.timezone = ZoneId.of(timezone).getId();
    this.notes = blankToNull(notes);
    this.active = active;
  }

  public void updateGuardian(String name, String whatsapp) {
    this.guardianName = blankToNull(name);
    this.guardianWhatsapp = blankToNull(whatsapp);
  }

  public String getGuardianName() {
    return guardianName;
  }

  public String getGuardianWhatsapp() {
    return guardianWhatsapp;
  }

  /** Registra quem declarou (ou revogou) o consentimento de gravação e quando. */
  public void recordConsent(boolean consent, UUID recordedBy, Instant at) {
    this.recordingConsent = consent;
    this.consentRecordedBy = Objects.requireNonNull(recordedBy, "recordedBy");
    this.consentRecordedAt = Objects.requireNonNull(at, "at");
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public UUID getTutorId() {
    return tutorId;
  }

  public String getName() {
    return name;
  }

  public String getPreferredName() {
    return preferredName;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public ZoneId getZoneId() {
    return ZoneId.of(timezone);
  }

  public String getNotes() {
    return notes;
  }

  public boolean hasRecordingConsent() {
    return recordingConsent;
  }

  public Instant getConsentRecordedAt() {
    return consentRecordedAt;
  }

  public UUID getConsentRecordedBy() {
    return consentRecordedBy;
  }

  public boolean isActive() {
    return active;
  }

  public IdosoDTO toDTO() {
    return new IdosoDTO(
        getId(),
        tutorId,
        name,
        preferredName,
        phoneNumber,
        getZoneId(),
        recordingConsent,
        active,
        guardianName,
        guardianWhatsapp);
  }
}
