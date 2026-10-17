package br.com.cuida.calendar;

import br.com.cuida.call.AlertLevel;
import br.com.cuida.call.CallStatus;
import br.com.cuida.call.CallSummary;
import br.com.cuida.call.CallTrigger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detalhe de uma ligação: dados da tentativa e o resumo estruturado completo.
 *
 * @param summary resumo ({@code null} enquanto não processada)
 * @param audioUrl caminho do áudio nesta API ({@code null} se não houver)
 * @param audioPurged o áudio existiu, mas a retenção já o apagou
 */
record CallDetailResponse(
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
    String summaryText,
    CallSummary summary,
    boolean hasTranscript,
    String audioUrl,
    boolean audioPurged) {}
