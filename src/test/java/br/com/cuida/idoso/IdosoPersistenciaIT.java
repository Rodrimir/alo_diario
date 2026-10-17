package br.com.cuida.idoso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.idoso.model.Idoso;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class IdosoPersistenciaIT {

  @Autowired IdosoRepository idosoRepository;

  @Autowired JdbcTemplate jdbc;

  @Test
  void persistsAndFindsByTutor() {
    UUID tutorId = TestFixtures.insertTutor(jdbc);
    Idoso saved =
        idosoRepository.save(
            new Idoso(tutorId, "José Pereira", "Seu Zé", "+5521998765432", "America/Recife"));

    Idoso loaded = idosoRepository.findByIdAndTutorId(saved.getId(), tutorId).orElseThrow();

    assertThat(loaded.getZoneId()).isEqualTo(ZoneId.of("America/Recife"));
    assertThat(loaded.hasRecordingConsent()).isFalse();
    assertThat(loaded.isActive()).isTrue();
    assertThat(idosoRepository.findByTutorIdOrderByNameAsc(tutorId)).hasSize(1);
    assertThat(idosoRepository.findByIdAndTutorId(saved.getId(), UUID.randomUUID())).isEmpty();
  }

  @Test
  void databaseRejectsNonE164Phone() {
    UUID tutorId = TestFixtures.insertTutor(jdbc);

    assertThatThrownBy(
            () ->
                idosoRepository.saveAndFlush(
                    new Idoso(tutorId, "José", null, "11 98765-4321", "America/Sao_Paulo")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
