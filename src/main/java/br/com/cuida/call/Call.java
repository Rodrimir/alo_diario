package br.com.cuida.call;

import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import br.com.cuida.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Uma tentativa de ligação para uma pessoa idosa. Retentativas são novas linhas com {@code attempt}
 * incrementado e {@code trigger = RETRY}.
 */
@Entity
@Table(name = "calls")
class Call extends AuditableEntity {

  @Column(name = "elderly_id", nullable = false, updatable = false)
  private UUID elderlyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, updatable = false, length = 20)
  private CallTrigger trigger;

  @Column(name = "scheduled_for", nullable = false)
  private Instant scheduledFor;

  /** Data no fuso da pessoa idosa: chave do calendário. */
  @Column(name = "local_date", nullable = false, updatable = false)
  private LocalDate localDate;

  @Column(nullable = false, updatable = false)
  private int attempt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CallStatus status;

  @Column(length = 20)
  private String provider;

  @Column(name = "provider_call_id", length = 100, unique = true)
  private String providerCallId;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "duration_seconds")
  private Integer durationSeconds;

  @Column(name = "end_reason", length = 120)
  private String endReason;

  /** URL da gravação no provedor; descartada depois de copiada para o nosso storage. */
  @Column(name = "provider_recording_url", columnDefinition = "text")
  private String providerRecordingUrl;

  @Column(name = "audio_object_key", length = 300)
  private String audioObjectKey;

  @Column(columnDefinition = "text")
  private String transcript;

  @Enumerated(EnumType.STRING)
  @Column(name = "transcript_source", length = 20)
  private TranscriptSource transcriptSource;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "questionnaire_snapshot")
  private QuestionnaireSnapshot questionnaireSnapshot;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "structured_summary")
  private CallSummary structuredSummary;

  @Column(name = "summary_text", columnDefinition = "text")
  private String summaryText;

  @Enumerated(EnumType.STRING)
  @Column(name = "alert_level", length = 20)
  private AlertLevel alertLevel;

  @Column(name = "failure_reason", length = 200)
  private String failureReason;

  @Version
  @Column(nullable = false)
  private long version;

  /** Exigido pelo JPA. */
  protected Call() {}

  Call(
      UUID elderlyId,
      CallTrigger trigger,
      Instant scheduledFor,
      LocalDate localDate,
      int attempt,
      QuestionnaireSnapshot questionnaireSnapshot) {
    super(UUID.randomUUID());
    this.elderlyId = Objects.requireNonNull(elderlyId, "elderlyId");
    this.trigger = Objects.requireNonNull(trigger, "trigger");
    this.scheduledFor = Objects.requireNonNull(scheduledFor, "scheduledFor");
    this.localDate = Objects.requireNonNull(localDate, "localDate");
    if (attempt < 1) {
      throw new IllegalArgumentException("attempt deve ser >= 1");
    }
    this.attempt = attempt;
    this.questionnaireSnapshot = questionnaireSnapshot;
    this.status = CallStatus.SCHEDULED;
  }

  void recordSummary(CallSummary summary) {
    this.structuredSummary = Objects.requireNonNull(summary, "summary");
    this.summaryText = summary.overview();
    this.alertLevel = summary.alertLevel();
  }

  UUID getElderlyId() {
    return elderlyId;
  }

  CallTrigger getTrigger() {
    return trigger;
  }

  Instant getScheduledFor() {
    return scheduledFor;
  }

  LocalDate getLocalDate() {
    return localDate;
  }

  int getAttempt() {
    return attempt;
  }

  CallStatus getStatus() {
    return status;
  }

  String getProvider() {
    return provider;
  }

  String getProviderCallId() {
    return providerCallId;
  }

  Instant getStartedAt() {
    return startedAt;
  }

  Instant getEndedAt() {
    return endedAt;
  }

  Integer getDurationSeconds() {
    return durationSeconds;
  }

  String getEndReason() {
    return endReason;
  }

  String getProviderRecordingUrl() {
    return providerRecordingUrl;
  }

  String getAudioObjectKey() {
    return audioObjectKey;
  }

  String getTranscript() {
    return transcript;
  }

  TranscriptSource getTranscriptSource() {
    return transcriptSource;
  }

  QuestionnaireSnapshot getQuestionnaireSnapshot() {
    return questionnaireSnapshot;
  }

  CallSummary getStructuredSummary() {
    return structuredSummary;
  }

  String getSummaryText() {
    return summaryText;
  }

  AlertLevel getAlertLevel() {
    return alertLevel;
  }

  String getFailureReason() {
    return failureReason;
  }

  long getVersion() {
    return version;
  }
}
