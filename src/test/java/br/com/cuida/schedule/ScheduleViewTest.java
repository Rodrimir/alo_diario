package br.com.cuida.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScheduleViewTest {

  private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
  private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

  @Test
  void dueAtUsesElderlyTimezone() {
    ScheduleView view = schedule(LocalTime.of(9, 30), EnumSet.allOf(DayOfWeek.class));

    assertThat(view.dueAt(LocalDate.of(2026, 10, 16), SAO_PAULO))
        .contains(Instant.parse("2026-10-16T12:30:00Z"));
  }

  @Test
  void dueAtIsEmptyOnDaysOutsideTheSchedule() {
    ScheduleView view = schedule(LocalTime.of(9, 0), EnumSet.of(DayOfWeek.MONDAY));

    // 16/10/2026 é sexta-feira.
    assertThat(view.dueAt(LocalDate.of(2026, 10, 16), SAO_PAULO)).isEmpty();
  }

  @Test
  void dstGapPushesTheTimeForward() {
    // Em Nova York, 08/03/2026 pula de 02:00 para 03:00.
    ScheduleView view = schedule(LocalTime.of(2, 30), EnumSet.allOf(DayOfWeek.class));

    assertThat(view.dueAt(LocalDate.of(2026, 3, 8), NEW_YORK))
        .contains(Instant.parse("2026-03-08T07:30:00Z"));
  }

  @Test
  void nextOccurrenceSkipsToTheNextScheduledDay() {
    ScheduleView view = schedule(LocalTime.of(9, 0), EnumSet.of(DayOfWeek.MONDAY));

    // Sexta, 16/10/2026 10:00 em São Paulo -> próxima segunda, 19/10 às 9h.
    assertThat(view.nextOccurrence(Instant.parse("2026-10-16T13:00:00Z"), SAO_PAULO))
        .hasValueSatisfying(
            next ->
                assertThat(next.toLocalDateTime())
                    .isEqualTo(LocalDate.of(2026, 10, 19).atTime(9, 0)));
  }

  @Test
  void inactiveScheduleHasNoNextOccurrence() {
    ScheduleView view =
        new ScheduleView(
            UUID.randomUUID(), LocalTime.of(9, 0), EnumSet.allOf(DayOfWeek.class), false, 3, 20);

    assertThat(view.nextOccurrence(Instant.now(), SAO_PAULO)).isEmpty();
  }

  private static ScheduleView schedule(LocalTime time, EnumSet<DayOfWeek> days) {
    return new ScheduleView(UUID.randomUUID(), time, days, true, 3, 20);
  }
}
