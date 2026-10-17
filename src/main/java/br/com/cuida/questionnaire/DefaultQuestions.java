package br.com.cuida.questionnaire;

import java.util.List;

/** Perguntas sugeridas criadas com o cadastro do idoso. O tutor pode trocar todas. */
final class DefaultQuestions {

  record Question(String attributeKey, String questionText, AnswerType answerType) {}

  static final List<Question> ITEMS =
      List.of(
          new Question("sleep", "Como dormiu esta noite?", AnswerType.FREE_TEXT),
          new Question("medication", "Tomou os remédios de hoje?", AnswerType.YES_NO),
          new Question("meals", "Já comeu hoje? O que comeu?", AnswerType.FREE_TEXT),
          new Question("pain", "Está sentindo alguma dor? De 1 a 5, quanto?", AnswerType.SCALE_1_5),
          new Question("mood", "Como está se sentindo hoje?", AnswerType.FREE_TEXT),
          new Question(
              "social_contact", "Conversou ou vai encontrar alguém hoje?", AnswerType.FREE_TEXT));

  private DefaultQuestions() {}
}
