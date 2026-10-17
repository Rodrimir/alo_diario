package br.com.cuida.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Agenda do idoso.
 *
 * @param nextCallAt próxima ligação agendada, no fuso do idoso ({@code null} se inativa)
 */
record ScheduleResponse(
    UUID elderlyId,
    @JsonFormat(pattern = "HH:mm") LocalTime localTime,
    List<DayOfWeek> daysOfWeek,
    boolean active,
    int maxAttempts,
    int retryIntervalMinutes,
    LocalDateTime nextCallAt) {}
