package br.com.cuida.questionario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.questionario.model.ItemQuestionario;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import br.com.cuida.questionario.model.TipoResposta;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class ItemQuestionarioPersistenciaIT {

  @Autowired ItemQuestionarioRepository itemQuestionarioRepository;

  @Autowired JdbcTemplate jdbc;

  @Test
  void listsItemsOrderedByPosition() {
    UUID elderlyId = TestFixtures.insertElderly(jdbc, TestFixtures.insertTutor(jdbc));
    itemQuestionarioRepository.saveAll(
        List.of(
            new ItemQuestionario(
                elderlyId, "medication", "Tomou os remédios?", TipoResposta.YES_NO, 1, true),
            new ItemQuestionario(
                elderlyId, "sleep", "Dormiu bem?", TipoResposta.SCALE_1_5, 0, true)));

    List<ItemQuestionario> loaded =
        itemQuestionarioRepository.findByElderlyIdOrderByPositionAsc(elderlyId);

    assertThat(loaded)
        .extracting(ItemQuestionario::getAttributeKey)
        .containsExactly("sleep", "medication");
    assertThat(loaded.getFirst().toSnapshotItem())
        .isEqualTo(
            new QuestionarioSnapshot.Item("sleep", "Dormiu bem?", TipoResposta.SCALE_1_5, 0));
  }

  @Test
  void rejectsDuplicateAttributeKeyForSameElderly() {
    UUID elderlyId = TestFixtures.insertElderly(jdbc, TestFixtures.insertTutor(jdbc));
    itemQuestionarioRepository.saveAndFlush(
        new ItemQuestionario(elderlyId, "sleep", "Dormiu bem?", TipoResposta.YES_NO, 0, true));

    assertThatThrownBy(
            () ->
                itemQuestionarioRepository.saveAndFlush(
                    new ItemQuestionario(
                        elderlyId, "sleep", "Dormiu?", TipoResposta.YES_NO, 1, true)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
