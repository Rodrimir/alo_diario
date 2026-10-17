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
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Uma tentativa de ligação para uma pessoa idosa. Retentativas são novas linhas com {@code attempt}
 * incrementado e {@code trigger = RETRY}.
 *
 * <pre>
 * SCHEDULED → DIALING → IN_PROGRESS → COMPLETED → PROCESSING → PROCESSED | PROCESSING_FAILED
 *                    ↘ NO_ANSWER | BUSY | FAILED
 * </pre>
 */
@Entity
@Table(name = "calls")
class Call extends AuditableEntity {

  static final Set<CallStatus> ACTIVE = EnumSet.of(CallStatus.DIALING, CallStatus.IN_PROGRESS);

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

  @Column(name = "audio_mime_type", length = 100)
  private String audioMimeType;

  @Column(name = "audio_size_bytes")
  private Long audioSizeBytes;

  /** Quando a retenção apagou o áudio ({@code audio_object_key} volta a ser {@code null}). */
  @Column(name = "audio_purged_at")
  private Instant audioPurgedAt;

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

  /** Dia em que o agendador não pôde ligar (ex.: sem consentimento): fica registrado como falha. */
  static Call notPlaced(
      UUID elderlyId, Instant scheduledFor, LocalDate localDate, String failureReason) {
    Call call = new Call(elderlyId, CallTrigger.SCHEDULED, scheduledFor, localDate, 1, null);
    call.fail(failureReason, scheduledFor);
    return call;
  }

  boolean isActive() {
    return ACTIVE.contains(status);
  }

  void markInProgress(Instant at) {
    if (status == CallStatus.DIALING) {
      status = CallStatus.IN_PROGRESS;
    }
    if (startedAt == null) {
      startedAt = at;
    }
  }

  /**
   * Aceita o relatório final enquanto a ligação está em andamento, e também se ela foi dada como
   * perdida por falta de relatório (o relatório atrasado recupera o dia).
   */
  boolean acceptsReport() {
    return isActive()
        || (status == CallStatus.FAILED && FailureReasons.NO_PROVIDER_REPORT.equals(failureReason));
  }

  void recordReport(ProviderCallEvent.Report report, CallStatus outcome, String failure) {
    this.status = outcome;
    this.startedAt = report.startedAt() != null ? report.startedAt() : startedAt;
    this.endedAt = report.endedAt();
    this.durationSeconds = report.durationSeconds();
    this.endReason = truncate(report.endedReason(), 120);
    this.failureReason = failure;
    if (report.transcript() != null && !report.transcript().isBlank()) {
      this.transcript = report.transcript();
      this.transcriptSource = TranscriptSource.PROVIDER;
    }
    this.providerRecordingUrl = report.recordingUrl();
  }

  void fail(String reason, Instant at) {
    this.status = CallStatus.FAILED;
    this.failureReason = truncate(reason, 200);
    if (endedAt == null) {
      endedAt = at;
    }
  }

  /** COMPLETED → PROCESSING. Também aceita PROCESSING (reprocesso depois de queda no meio). */
  boolean startProcessing() {
    if (status != CallStatus.COMPLETED && status != CallStatus.PROCESSING) {
      return false;
    }
    status = CallStatus.PROCESSING;
    failureReason = null;
    return true;
  }

  void attachAudio(String objectKey, String mimeType, long sizeBytes) {
    this.audioObjectKey = Objects.requireNonNull(objectKey, "objectKey");
    this.audioMimeType = mimeType;
    this.audioSizeBytes = sizeBytes;
    this.audioPurgedAt = null;
    // A URL do provedor não é mais necessária (e expira); não guardamos cópia dela.
    this.providerRecordingUrl = null;
  }

  void attachTranscript(String transcript, TranscriptSource source) {
    this.transcript = transcript;
    this.transcriptSource = source;
  }

  void recordSummary(CallSummary summary) {
    this.structuredSummary = Objects.requireNonNull(summary, "summary");
    this.summaryText = summary.overview();
    this.alertLevel = summary.alertLevel();
  }

  void markProcessed(CallSummary summary) {
    recordSummary(summary);
    this.status = CallStatus.PROCESSED;
    this.failureReason = null;
  }

  void markProcessingFailed(String reason) {
    this.status = CallStatus.PROCESSING_FAILED;
    this.failureReason = truncate(reason, 200);
  }

  /** Volta para COMPLETED para o pipeline rodar de novo (reprocesso manual). */
  boolean resetForReprocessing() {
    if (status != CallStatus.PROCESSED && status != CallStatus.PROCESSING_FAILED) {
      return false;
    }
    status = CallStatus.COMPLETED;
    failureReason = null;
    return true;
  }

  void markAudioPurged(Instant at) {
    this.audioObjectKey = null;
    this.audioMimeType = null;
    this.audioSizeBytes = null;
    this.audioPurgedAt = at;
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() > max ? value.substring(0, max) : value;
  }

  CallView toView() {
    return new CallView(
        getId(),
        elderlyId,
        trigger,
        status,
        scheduledFor,
        localDate,
        attempt,
        provider,
        providerCallId,
        startedAt,
        endedAt,
        durationSeconds,
        endReason,
        failureReason,
        transcript,
        transcriptSource,
        providerRecordingUrl,
        audioObjectKey,
        audioMimeType,
        audioSizeBytes,
        audioPurgedAt,
        questionnaireSnapshot,
        structuredSummary,
        summaryText,
        alertLevel,
        getCreatedAt(),
        getUpdatedAt());
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

  String getAudioMimeType() {
    return audioMimeType;
  }

  Long getAudioSizeBytes() {
    return audioSizeBytes;
  }

  Instant getAudioPurgedAt() {
    return audioPurgedAt;
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
