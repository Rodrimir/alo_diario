package br.com.cuida.agenda.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Agenda de um idoso, para uso dos outros módulos (API pública).
 *
 * @param localTime horário no fuso do idoso
 * @param maxAttempts tentativas automáticas por dia (1 a 5)
 * @param retryIntervalMinutes intervalo entre tentativas (5 a 240 minutos)
 */
public record AgendaDTO(
    UUID elderlyId,
    LocalTime localTime,
    Set<DayOfWeek> daysOfWeek,
    boolean active,
    int maxAttempts,
    int retryIntervalMinutes) {

  public AgendaDTO {
    daysOfWeek = Set.copyOf(daysOfWeek);
  }

  /**
   * Instante em que a ligação do dia {@code date} (no fuso {@code zone}) vence, ou vazio se a
   * agenda não liga nesse dia da semana. Em um "buraco" de horário de verão (ex.: 00:30 que não
   * existe), a hora é empurrada para frente, como faz {@link ZonedDateTime#of}.
   */
  public Optional<Instant> dueAt(LocalDate date, ZoneId zone) {
    if (!daysOfWeek.contains(date.getDayOfWeek())) {
      return Optional.empty();
    }
    return Optional.of(ZonedDateTime.of(date, localTime, zone).toInstant());
  }

  /** Próxima ligação agendada a partir de {@code now}, ou vazio se inativa/sem dias. */
  public Optional<ZonedDateTime> nextOccurrence(Instant now, ZoneId zone) {
    if (!active || daysOfWeek.isEmpty()) {
      return Optional.empty();
    }
    LocalDate date = LocalDate.ofInstant(now, zone);
    for (int i = 0; i <= 7; i++) {
      LocalDate candidate = date.plusDays(i);
      Optional<Instant> due = dueAt(candidate, zone);
      if (due.isPresent() && !due.get().isBefore(now)) {
        return Optional.of(due.get().atZone(zone));
      }
    }
    return Optional.empty();
  }
}
