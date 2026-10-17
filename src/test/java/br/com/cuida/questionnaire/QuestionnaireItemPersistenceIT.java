package br.com.cuida.questionnaire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class QuestionnaireItemPersistenceIT {

  @Autowired QuestionnaireItemRepository items;

  @Autowired JdbcTemplate jdbc;

  @Test
  void listsItemsOrderedByPosition() {
    UUID elderlyId = TestFixtures.insertElderly(jdbc, TestFixtures.insertTutor(jdbc));
    items.saveAll(
        List.of(
            new QuestionnaireItem(
                elderlyId, "medication", "Tomou os remédios?", AnswerType.YES_NO, 1, true),
            new QuestionnaireItem(
                elderlyId, "sleep", "Dormiu bem?", AnswerType.SCALE_1_5, 0, true)));

    List<QuestionnaireItem> loaded = items.findByElderlyIdOrderByPositionAsc(elderlyId);

    assertThat(loaded)
        .extracting(QuestionnaireItem::getAttributeKey)
        .containsExactly("sleep", "medication");
    assertThat(loaded.getFirst().toSnapshotItem())
        .isEqualTo(new QuestionnaireSnapshot.Item("sleep", "Dormiu bem?", AnswerType.SCALE_1_5, 0));
  }

  @Test
  void rejectsDuplicateAttributeKeyForSameElderly() {
    UUID elderlyId = TestFixtures.insertElderly(jdbc, TestFixtures.insertTutor(jdbc));
    items.saveAndFlush(
        new QuestionnaireItem(elderlyId, "sleep", "Dormiu bem?", AnswerType.YES_NO, 0, true));

    assertThatThrownBy(
            () ->
                items.saveAndFlush(
                    new QuestionnaireItem(
                        elderlyId, "sleep", "Dormiu?", AnswerType.YES_NO, 1, true)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
