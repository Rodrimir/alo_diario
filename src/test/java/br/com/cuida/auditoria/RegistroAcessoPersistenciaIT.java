package br.com.cuida.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.auditoria.model.AcaoAuditada;
import br.com.cuida.auditoria.model.RegistroAcesso;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class RegistroAcessoPersistenciaIT {

  @Autowired RegistroAcessoRepository registroAcessoRepository;

  @Autowired JdbcTemplate jdbc;

  @Test
  void keepsAuditTrailWhenElderlyIsDeleted() {
    UUID tutorId = TestFixtures.insertTutor(jdbc);
    UUID elderlyId = TestFixtures.insertElderly(jdbc, tutorId);
    RegistroAcesso log =
        registroAcessoRepository.save(
            new RegistroAcesso(
                tutorId,
                elderlyId,
                null,
                AcaoAuditada.VIEW_TRANSCRIPT,
                "127.0.0.1",
                Instant.now()));

    jdbc.update("delete from elderly where id = ?", elderlyId);

    RegistroAcesso loaded = registroAcessoRepository.findById(log.getId()).orElseThrow();
    assertThat(loaded.getElderlyId()).isNull();
    assertThat(loaded.getUserId()).isEqualTo(tutorId);
    assertThat(loaded.getAction()).isEqualTo(AcaoAuditada.VIEW_TRANSCRIPT);
  }
}
