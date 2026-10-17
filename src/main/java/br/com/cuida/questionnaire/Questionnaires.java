package br.com.cuida.questionnaire;

import br.com.cuida.elderly.ElderlyRegistered;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública do módulo: o snapshot do questionário vigente, gravado em cada ligação. */
@Service
@Transactional(readOnly = true)
public class Questionnaires {

  private final QuestionnaireItemRepository repository;

  Questionnaires(QuestionnaireItemRepository repository) {
    this.repository = repository;
  }

  /** Perguntas ativas, na ordem em que devem ser feitas. */
  public QuestionnaireSnapshot snapshot(UUID elderlyId) {
    List<QuestionnaireSnapshot.Item> items =
        repository.findByElderlyIdOrderByPositionAsc(elderlyId).stream()
            .filter(QuestionnaireItem::isActive)
            .map(QuestionnaireItem::toSnapshotItem)
            .toList();
    return new QuestionnaireSnapshot(items);
  }

  /** Todo idoso nasce com as perguntas sugeridas. */
  @EventListener
  @Transactional
  void on(ElderlyRegistered event) {
    if (repository.existsByElderlyId(event.elderlyId())) {
      return;
    }
    int position = 0;
    for (DefaultQuestions.Question question : DefaultQuestions.ITEMS) {
      repository.save(
          new QuestionnaireItem(
              event.elderlyId(),
              question.attributeKey(),
              question.questionText(),
              question.answerType(),
              position++,
              true));
    }
  }
}
