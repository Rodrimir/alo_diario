package br.com.cuida.ligacao.model;

import br.com.cuida.infra.util.HorarioLocal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Ligação como sai na API (lista do dia, disparo manual). Datas no fuso do idoso. O resumo
 * completo, a transcrição e o áudio têm endpoints próprios.
 */
public record LigacaoResponseDTO(
    UUID id,
    UUID elderlyId,
    OrigemLigacao trigger,
    StatusLigacao status,
    LocalDate localDate,
    int attempt,
    LocalDateTime scheduledFor,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Integer durationSeconds,
    String endReason,
    String failureReason,
    NivelAlerta alertLevel,
    Humor mood,
    String summaryText,
    boolean hasTranscript,
    boolean hasAudio) {

  public static LigacaoResponseDTO of(LigacaoDTO call, ZoneId zone) {
    return new LigacaoResponseDTO(
        call.id(),
        call.elderlyId(),
        call.trigger(),
        call.status(),
        call.localDate(),
        call.attempt(),
        HorarioLocal.at(call.scheduledFor(), zone),
        HorarioLocal.at(call.startedAt(), zone),
        HorarioLocal.at(call.endedAt(), zone),
        call.durationSeconds(),
        call.endReason(),
        call.failureReason(),
        call.alertLevel(),
        call.mood(),
        call.summaryText(),
        call.hasTranscript(),
        call.hasAudio());
  }
}
