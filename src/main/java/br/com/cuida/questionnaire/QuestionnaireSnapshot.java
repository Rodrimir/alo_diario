package br.com.cuida.questionnaire;

import java.util.List;

/**
 * Cópia imutável do questionário vigente no momento em que uma ligação foi criada. Fica gravada na
 * própria ligação, preservando o histórico mesmo que o tutor altere as perguntas depois.
 */
public record QuestionnaireSnapshot(List<Item> items) {

  public QuestionnaireSnapshot {
    items = List.copyOf(items);
  }

  /** Uma pergunta congelada no snapshot. */
  public record Item(
      String attributeKey, String questionText, AnswerType answerType, int position) {}
}
