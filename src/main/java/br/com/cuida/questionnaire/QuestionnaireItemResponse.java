package br.com.cuida.questionnaire;

import java.util.UUID;

/** Pergunta do questionário de um idoso. */
record QuestionnaireItemResponse(
    UUID id,
    String attributeKey,
    String questionText,
    AnswerType answerType,
    int position,
    boolean active) {

  static QuestionnaireItemResponse of(QuestionnaireItem item) {
    return new QuestionnaireItemResponse(
        item.getId(),
        item.getAttributeKey(),
        item.getQuestionText(),
        item.getAnswerType(),
        item.getPosition(),
        item.isActive());
  }
}
