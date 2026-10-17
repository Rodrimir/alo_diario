package br.com.cuida.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.agenda.model.Agenda;
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
class AgendaPersistenciaIT {

  @Autowired AgendaRepository agendaRepository;

  @Autowired JdbcTemplate jdbc;

  @Test
  void storesDaysOfWeekAsJsonbArrayOfNames() {
    UUID elderlyId = TestFixtures.insertElderly(jdbc, TestFixtures.insertTutor(jdbc));
    var days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    agendaRepository.save(new Agenda(elderlyId, LocalTime.of(9, 30), days, true));

    Agenda loaded = agendaRepository.findByElderlyId(elderlyId).orElseThrow();
    String raw =
        jdbc.queryForObject(
            "select days_of_week::text from call_schedules where elderly_id = ?",
            String.class,
            elderlyId);

    assertThat(loaded.getDaysOfWeek()).containsExactlyInAnyOrderElementsOf(days);
    assertThat(loaded.getLocalTime()).isEqualTo(LocalTime.of(9, 30));
    assertThat(loaded.getMaxAttempts()).isEqualTo(Agenda.DEFAULT_MAX_ATTEMPTS);
    assertThat(loaded.getRetryIntervalMinutes()).isEqualTo(Agenda.DEFAULT_RETRY_INTERVAL_MINUTES);
    assertThat(raw).contains("\"MONDAY\"").contains("\"WEDNESDAY\"").contains("\"FRIDAY\"");
  }
}
