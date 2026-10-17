package br.com.cuida.agenda.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * Corpo de {@code PUT /api/elderly/{id}/schedule}.
 *
 * @param localTime horário no fuso do idoso, {@code "HH:mm"}
 * @param daysOfWeek {@code ["MONDAY", ...]}; obrigatório ter ao menos um dia se {@code active}
 * @param maxAttempts tentativas automáticas por dia; padrão 3
 * @param retryIntervalMinutes minutos entre tentativas; padrão 20
 */
public record AgendaRequestDTO(
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime localTime,
    @NotNull Set<@NotNull DayOfWeek> daysOfWeek,
    @NotNull Boolean active,
    @Min(1) @Max(5) Integer maxAttempts,
    @Min(5) @Max(240) Integer retryIntervalMinutes) {}
