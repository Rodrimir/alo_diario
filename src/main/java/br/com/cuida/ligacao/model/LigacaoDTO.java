package br.com.cuida.ligacao.model;

import br.com.cuida.questionario.model.QuestionarioSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados de uma ligação para os outros módulos (API pública). Datas em UTC; cada módulo converte
 * para o fuso do idoso na saída.
 */
public record LigacaoDTO(
    UUID id,
    UUID elderlyId,
    OrigemLigacao trigger,
    StatusLigacao status,
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
    OrigemTranscricao transcriptSource,
    String providerRecordingUrl,
    String audioObjectKey,
    String audioMimeType,
    Long audioSizeBytes,
    Instant audioPurgedAt,
    QuestionarioSnapshot questionnaireSnapshot,
    ResumoLigacao summary,
    String summaryText,
    NivelAlerta alertLevel,
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

  public Humor mood() {
    return summary != null ? summary.mood() : null;
  }
}
