package br.com.cuida.elderly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class ElderlyPersistenceIT {

  @Autowired ElderlyRepository elderly;

  @Autowired JdbcTemplate jdbc;

  @Test
  void persistsAndFindsByTutor() {
    UUID tutorId = TestFixtures.insertTutor(jdbc);
    Elderly saved =
        elderly.save(
            new Elderly(tutorId, "José Pereira", "Seu Zé", "+5521998765432", "America/Recife"));

    Elderly loaded = elderly.findByIdAndTutorId(saved.getId(), tutorId).orElseThrow();

    assertThat(loaded.getZoneId()).isEqualTo(ZoneId.of("America/Recife"));
    assertThat(loaded.hasRecordingConsent()).isFalse();
    assertThat(loaded.isActive()).isTrue();
    assertThat(elderly.findByTutorId(tutorId, PageRequest.of(0, 10))).hasSize(1);
    assertThat(elderly.findByIdAndTutorId(saved.getId(), UUID.randomUUID())).isEmpty();
  }

  @Test
  void databaseRejectsNonE164Phone() {
    UUID tutorId = TestFixtures.insertTutor(jdbc);

    assertThatThrownBy(
            () ->
                elderly.saveAndFlush(
                    new Elderly(tutorId, "José", null, "11 98765-4321", "America/Sao_Paulo")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
