package br.com.cuida.call;

import br.com.cuida.shared.time.LocalTimes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Ligação como sai na API (lista do dia, disparo manual). Datas no fuso do idoso. O resumo
 * completo, a transcrição e o áudio têm endpoints próprios.
 */
public record CallResponse(
    UUID id,
    UUID elderlyId,
    CallTrigger trigger,
    CallStatus status,
    LocalDate localDate,
    int attempt,
    LocalDateTime scheduledFor,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Integer durationSeconds,
    String endReason,
    String failureReason,
    AlertLevel alertLevel,
    Mood mood,
    String summaryText,
    boolean hasTranscript,
    boolean hasAudio) {

  public static CallResponse of(CallView call, ZoneId zone) {
    return new CallResponse(
        call.id(),
        call.elderlyId(),
        call.trigger(),
        call.status(),
        call.localDate(),
        call.attempt(),
        LocalTimes.at(call.scheduledFor(), zone),
        LocalTimes.at(call.startedAt(), zone),
        LocalTimes.at(call.endedAt(), zone),
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
