package br.com.cuida.calendar;

import br.com.cuida.call.AlertLevel;
import br.com.cuida.call.CallStatus;
import br.com.cuida.call.Mood;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um dia do calendário. Mostra a ligação mais relevante do dia (a última processada, senão a última
 * tentativa); {@code attempts} conta todas, inclusive as manuais.
 */
record CalendarDayResponse(
    LocalDate date,
    UUID callId,
    CallStatus status,
    AlertLevel alertLevel,
    Mood mood,
    String summaryText,
    int attempts,
    boolean hasTranscript,
    boolean hasAudio) {}
