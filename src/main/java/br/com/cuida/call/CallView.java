package br.com.cuida.call;

import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados de uma ligação para os outros módulos (API pública). Datas em UTC; cada módulo converte
 * para o fuso do idoso na saída.
 */
public record CallView(
    UUID id,
    UUID elderlyId,
    CallTrigger trigger,
    CallStatus status,
    Instant scheduledFor,
    LocalDate localDate,
    int attempt,
    String provider,
    String providerCallId,
    Instant startedAt,
    Instant endedAt,
    Integer durationSeconds,
    String endReason,
    String failureReason,
    String transcript,
    TranscriptSource transcriptSource,
    String providerRecordingUrl,
    String audioObjectKey,
    String audioMimeType,
    Long audioSizeBytes,
    Instant audioPurgedAt,
    QuestionnaireSnapshot questionnaireSnapshot,
    CallSummary summary,
    String summaryText,
    AlertLevel alertLevel,
    Instant createdAt,
    Instant updatedAt) {

  public boolean hasTranscript() {
    return transcript != null && !transcript.isBlank();
  }

  public boolean hasAudio() {
    return audioObjectKey != null;
  }

  /** O áudio existiu, mas a retenção já o apagou. */
  public boolean audioPurged() {
    return audioPurgedAt != null;
  }

  public Mood mood() {
    return summary != null ? summary.mood() : null;
  }
}
