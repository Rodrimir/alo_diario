package br.com.cuida.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class CallSchedulePersistenceIT {

  @Autowired CallScheduleRepository schedules;

  @Autowired JdbcTemplate jdbc;

  @Test
  void storesDaysOfWeekAsJsonbArrayOfNames() {
    UUID elderlyId = TestFixtures.insertElderly(jdbc, TestFixtures.insertTutor(jdbc));
    var days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    schedules.save(new CallSchedule(elderlyId, LocalTime.of(9, 30), days, true));

    CallSchedule loaded = schedules.findByElderlyId(elderlyId).orElseThrow();
    String raw =
        jdbc.queryForObject(
            "select days_of_week::text from call_schedules where elderly_id = ?",
            String.class,
            elderlyId);

    assertThat(loaded.getDaysOfWeek()).containsExactlyInAnyOrderElementsOf(days);
    assertThat(loaded.getLocalTime()).isEqualTo(LocalTime.of(9, 30));
    assertThat(loaded.getMaxAttempts()).isEqualTo(CallSchedule.DEFAULT_MAX_ATTEMPTS);
    assertThat(loaded.getRetryIntervalMinutes())
        .isEqualTo(CallSchedule.DEFAULT_RETRY_INTERVAL_MINUTES);
    assertThat(raw).contains("\"MONDAY\"").contains("\"WEDNESDAY\"").contains("\"FRIDAY\"");
  }
}
