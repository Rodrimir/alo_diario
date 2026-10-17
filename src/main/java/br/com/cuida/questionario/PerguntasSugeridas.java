package br.com.cuida.questionario;

import br.com.cuida.questionario.model.TipoResposta;
import java.util.List;

/** Perguntas sugeridas criadas com o cadastro do idoso. O responsável pode trocar todas. */
final class PerguntasSugeridas {

  record Question(String attributeKey, String questionText, TipoResposta answerType) {}

  /**
   * As perguntas do pitch do Alô Diário. A última ({@code message_for_family}) é a do recado: o
   * resumo a mostra como "Recado", fora do checklist.
   */
  static final List<Question> ITEMS =
      List.of(
          new Question("wellbeing", "Como você está se sentindo hoje?", TipoResposta.FREE_TEXT),
          new Question("medication", "Tomou os remédios de hoje?", TipoResposta.YES_NO),
          new Question("sleep", "Dormiu bem esta noite?", TipoResposta.YES_NO),
          new Question("needed_help", "Precisou de ajuda com alguma coisa?", TipoResposta.YES_NO),
          new Question(
              "suspicious_call",
              "Recebeu alguma ligação ou mensagem estranha ou suspeita?",
              TipoResposta.YES_NO),
          new Question(
              "message_for_family",
              "Gostaria de deixar algum recado para a sua família?",
              TipoResposta.FREE_TEXT));

  private PerguntasSugeridas() {}
}
