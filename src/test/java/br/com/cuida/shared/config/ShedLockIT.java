package br.com.cuida.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** O lock provider grava horários em UTC (colunas timestamp, V4) e impede execução dupla. */
@IntegrationTest
class ShedLockIT {

  @Autowired DataSource dataSource;

  @Autowired JdbcTemplate jdbc;

  @Test
  void locksWithUtcTimestampsAndBlocksSecondHolder() {
    var provider = new SchedulingConfig().lockProvider(dataSource);
    String name = "teste-" + UUID.randomUUID();
    var config = new LockConfiguration(Instant.now(), name, Duration.ofMinutes(5), Duration.ZERO);

    assertThat(provider.lock(config)).isPresent();
    assertThat(provider.lock(config)).isEmpty();

    LocalDateTime lockUntil =
        jdbc.queryForObject(
            "select lock_until from shedlock where name = ?", LocalDateTime.class, name);
    LocalDateTime expected = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5);
    assertThat(ChronoUnit.SECONDS.between(lockUntil, expected)).isBetween(-60L, 60L);
  }
}
