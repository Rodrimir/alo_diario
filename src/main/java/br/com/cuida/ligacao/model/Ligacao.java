package br.com.cuida.ligacao.model;

import br.com.cuida.infra.persistencia.EntidadeAuditavel;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
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
public class Ligacao extends EntidadeAuditavel {

  public static final Set<StatusLigacao> ACTIVE =
      EnumSet.of(StatusLigacao.DIALING, StatusLigacao.IN_PROGRESS);

  @Column(name = "elderly_id", nullable = false, updatable = false)
  private UUID elderlyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, updatable = false, length = 20)
  private OrigemLigacao trigger;

  @Column(name = "scheduled_for", nullable = false)
  private Instant scheduledFor;

  /** Data no fuso da pessoa idosa: chave do calendário. */
  @Column(name = "local_date", nullable = false, updatable = false)
  private LocalDate localDate;

  @Column(nullable = false, updatable = false)
  private int attempt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private StatusLigacao status;

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
  private OrigemTranscricao transcriptSource;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "questionnaire_snapshot")
  private QuestionarioSnapshot questionnaireSnapshot;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "structured_summary")
  private ResumoLigacao structuredSummary;

  @Column(name = "summary_text", columnDefinition = "text")
  private String summaryText;

  @Enumerated(EnumType.STRING)
  @Column(name = "alert_level", length = 20)
  private NivelAlerta alertLevel;

  @Column(name = "failure_reason", length = 200)
  private String failureReason;

  /** Esta tentativa já foi avaliada e não gera outra (retentativa criada ou encerrada). */
  @Column(name = "retry_closed", nullable = false)
  private boolean retryClosed;

  @Version
  @Column(nullable = false)
  private long version;

  /** Exigido pelo JPA. */
  protected Ligacao() {}

  public Ligacao(
      UUID elderlyId,
      OrigemLigacao trigger,
      Instant scheduledFor,
      LocalDate localDate,
      int attempt,
      QuestionarioSnapshot questionnaireSnapshot) {
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
    this.status = StatusLigacao.SCHEDULED;
  }

  /** Dia em que o agendador não pôde ligar (ex.: sem consentimento): fica registrado como falha. */
  public static Ligacao notPlaced(
      UUID elderlyId, Instant scheduledFor, LocalDate localDate, String failureReason) {
    Ligacao call =
        new Ligacao(elderlyId, OrigemLigacao.SCHEDULED, scheduledFor, localDate, 1, null);
    call.fail(failureReason, scheduledFor);
    return call;
  }

  /** Marca que esta tentativa não gera outra: o planejador de retentativas não a olha mais. */
  public void closeRetries() {
    this.retryClosed = true;
  }

  public boolean isRetryClosed() {
    return retryClosed;
  }

  public boolean isActive() {
    return ACTIVE.contains(status);
  }

  public void markInProgress(Instant at) {
    if (status == StatusLigacao.DIALING) {
      status = StatusLigacao.IN_PROGRESS;
    }
    if (startedAt == null) {
      startedAt = at;
    }
  }

  /**
   * Aceita o relatório final enquanto a ligação está em andamento, e também se ela foi dada como
   * perdida por falta de relatório (o relatório atrasado recupera o dia).
   */
  public boolean acceptsReport() {
    return isActive()
        || (status == StatusLigacao.FAILED
            && MotivosDeFalha.NO_PROVIDER_REPORT.equals(failureReason));
  }

  public void recordReport(EventoDoProvedor.Report report, StatusLigacao outcome, String failure) {
    this.status = outcome;
    this.startedAt = report.startedAt() != null ? report.startedAt() : startedAt;
    this.endedAt = report.endedAt();
    this.durationSeconds = report.durationSeconds();
    this.endReason = truncate(report.endedReason(), 120);
    this.failureReason = failure;
    if (report.transcript() != null && !report.transcript().isBlank()) {
      this.transcript = report.transcript();
      this.transcriptSource = OrigemTranscricao.PROVIDER;
    }
    this.providerRecordingUrl = report.recordingUrl();
  }

  public void fail(String reason, Instant at) {
    this.status = StatusLigacao.FAILED;
    this.failureReason = truncate(reason, 200);
    if (endedAt == null) {
      endedAt = at;
    }
  }

  /** COMPLETED → PROCESSING. Também aceita PROCESSING (reprocesso depois de queda no meio). */
  public boolean startProcessing() {
    if (status != StatusLigacao.COMPLETED && status != StatusLigacao.PROCESSING) {
      return false;
    }
    status = StatusLigacao.PROCESSING;
    failureReason = null;
    return true;
  }

  public void attachAudio(String objectKey, String mimeType, long sizeBytes) {
    this.audioObjectKey = Objects.requireNonNull(objectKey, "objectKey");
    this.audioMimeType = mimeType;
    this.audioSizeBytes = sizeBytes;
    this.audioPurgedAt = null;
    // A URL do provedor não é mais necessária (e expira); não guardamos cópia dela.
    this.providerRecordingUrl = null;
  }

  public void attachTranscript(String transcript, OrigemTranscricao source) {
    this.transcript = transcript;
    this.transcriptSource = source;
  }

  public void recordSummary(ResumoLigacao summary) {
    this.structuredSummary = Objects.requireNonNull(summary, "summary");
    this.summaryText = summary.overview();
    this.alertLevel = summary.alertLevel();
  }

  public void markProcessed(ResumoLigacao summary) {
    recordSummary(summary);
    this.status = StatusLigacao.PROCESSED;
    this.failureReason = null;
  }

  public void markProcessingFailed(String reason) {
    this.status = StatusLigacao.PROCESSING_FAILED;
    this.failureReason = truncate(reason, 200);
  }

  /** Volta para COMPLETED para o pipeline rodar de novo (reprocesso manual). */
  public boolean resetForReprocessing() {
    if (status != StatusLigacao.PROCESSED && status != StatusLigacao.PROCESSING_FAILED) {
      return false;
    }
    status = StatusLigacao.COMPLETED;
    failureReason = null;
    return true;
  }

  public void markAudioPurged(Instant at) {
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

  public LigacaoDTO toDTO() {
    return new LigacaoDTO(
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

  public UUID getElderlyId() {
    return elderlyId;
  }

  public OrigemLigacao getTrigger() {
    return trigger;
  }

  public Instant getScheduledFor() {
    return scheduledFor;
  }

  public LocalDate getLocalDate() {
    return localDate;
  }

  public int getAttempt() {
    return attempt;
  }

  public StatusLigacao getStatus() {
    return status;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderCallId() {
    return providerCallId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public Integer getDurationSeconds() {
    return durationSeconds;
  }

  public String getEndReason() {
    return endReason;
  }

  public String getProviderRecordingUrl() {
    return providerRecordingUrl;
  }

  public String getAudioObjectKey() {
    return audioObjectKey;
  }

  public String getAudioMimeType() {
    return audioMimeType;
  }

  public Long getAudioSizeBytes() {
    return audioSizeBytes;
  }

  public Instant getAudioPurgedAt() {
    return audioPurgedAt;
  }

  public String getTranscript() {
    return transcript;
  }

  public OrigemTranscricao getTranscriptSource() {
    return transcriptSource;
  }

  public QuestionarioSnapshot getQuestionnaireSnapshot() {
    return questionnaireSnapshot;
  }

  public ResumoLigacao getStructuredSummary() {
    return structuredSummary;
  }

  public String getSummaryText() {
    return summaryText;
  }

  public NivelAlerta getAlertLevel() {
    return alertLevel;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public long getVersion() {
    return version;
  }
}
