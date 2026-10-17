package br.com.cuida.questionario;

import br.com.cuida.questionario.model.TipoResposta;
import java.util.List;

/** Perguntas sugeridas criadas com o cadastro do idoso. O tutor pode trocar todas. */
final class PerguntasSugeridas {

  record Question(String attributeKey, String questionText, TipoResposta answerType) {}

  static final List<Question> ITEMS =
      List.of(
          new Question("sleep", "Como dormiu esta noite?", TipoResposta.FREE_TEXT),
          new Question("medication", "Tomou os remédios de hoje?", TipoResposta.YES_NO),
          new Question("meals", "Já comeu hoje? O que comeu?", TipoResposta.FREE_TEXT),
          new Question(
              "pain", "Está sentindo alguma dor? De 1 a 5, quanto?", TipoResposta.SCALE_1_5),
          new Question("mood", "Como está se sentindo hoje?", TipoResposta.FREE_TEXT),
          new Question(
              "social_contact", "Conversou ou vai encontrar alguém hoje?", TipoResposta.FREE_TEXT));

  private PerguntasSugeridas() {}
}
