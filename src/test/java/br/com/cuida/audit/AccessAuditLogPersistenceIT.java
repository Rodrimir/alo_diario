package br.com.cuida.audit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class AccessAuditLogPersistenceIT {

  @Autowired AccessAuditLogRepository logs;

  @Autowired JdbcTemplate jdbc;

  @Test
  void keepsAuditTrailWhenElderlyIsDeleted() {
    UUID tutorId = TestFixtures.insertTutor(jdbc);
    UUID elderlyId = TestFixtures.insertElderly(jdbc, tutorId);
    AccessAuditLog log =
        logs.save(
            new AccessAuditLog(
                tutorId, elderlyId, null, AuditAction.VIEW_TRANSCRIPT, "127.0.0.1", Instant.now()));

    jdbc.update("delete from elderly where id = ?", elderlyId);

    AccessAuditLog loaded = logs.findById(log.getId()).orElseThrow();
    assertThat(loaded.getElderlyId()).isNull();
    assertThat(loaded.getUserId()).isEqualTo(tutorId);
    assertThat(loaded.getAction()).isEqualTo(AuditAction.VIEW_TRANSCRIPT);
  }
}
