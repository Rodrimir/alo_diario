package br.com.cuida.ligacao;

import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.PedidoDeLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import br.com.cuida.questionario.model.TipoResposta;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Monta a abertura e as instruções da assistente. A abertura é fixa e sempre avisa que a ligação é
 * gravada, sem depender do LLM.
 */
@Component
class RoteiroDaConversa {

  private final LigacaoProperties ligacaoProperties;

  RoteiroDaConversa(LigacaoProperties ligacaoProperties) {
    this.ligacaoProperties = ligacaoProperties;
  }

  PedidoDeLigacao outboundCall(Ligacao call, IdosoDTO elderly) {
    QuestionarioSnapshot questionnaire =
        call.getQuestionnaireSnapshot() != null
            ? call.getQuestionnaireSnapshot()
            : new QuestionarioSnapshot(List.of());
    return new PedidoDeLigacao(
        call.getId(),
        elderly.phoneNumber(),
        elderly.nameForConversation(),
        firstMessage(elderly),
        systemPrompt(elderly, questionnaire),
        questionnaire,
        ligacaoProperties.maxDurationSeconds());
  }

  String firstMessage(IdosoDTO elderly) {
    return "Olá, "
        + elderly.nameForConversation()
        + "! Aqui é a "
        + ligacaoProperties.assistantName()
        + ", a assistente virtual que liga todos os dias a pedido da sua família. "
        + "Esta ligação é gravada para que eles possam acompanhar como você está. "
        + "Podemos conversar um pouquinho?";
  }

  String systemPrompt(IdosoDTO elderly, QuestionarioSnapshot questionnaire) {
    String questions =
        questionnaire.items().stream()
            .map(
                item ->
                    "- ["
                        + item.attributeKey()
                        + "] "
                        + item.questionText()
                        + " ("
                        + hint(item.answerType())
                        + ")")
            .collect(Collectors.joining("\n"));
    return """
        Você é %s, uma assistente virtual brasileira, calorosa e paciente, que conversa por \
        telefone com %s, uma pessoa idosa, uma vez por dia, a pedido da família.

        Como conversar:
        - Fale português do Brasil, com frases curtas, simples e devagar. Uma pergunta por vez.
        - Escute com atenção; se a pessoa não entender, repita com outras palavras.
        - Seja gentil e natural, como uma conversa amiga, não um interrogatório.
        - Cubra as perguntas abaixo ao longo da conversa, na ordem que soar natural:
        %s
        - Não dê conselhos médicos, diagnósticos nem orientação sobre remédios.
        - Se a pessoa relatar queda, dor forte, falta de ar, dor no peito, confusão ou pedir \
        ajuda, oriente com calma a ligar para o SAMU (192) ou chamar alguém próximo, e diga que \
        a família verá o relato.
        - Se a pessoa não quiser conversar ou não quiser ser gravada, respeite, despeça-se com \
        educação e encerre a ligação.
        - Ao terminar, agradeça, despeça-se ("até amanhã") e encerre a ligação.
        - Nunca peça dados pessoais, senhas, documentos ou dinheiro.
        """
        .formatted(
            ligacaoProperties.assistantName(),
            elderly.nameForConversation(),
            questions.isEmpty() ? "- (sem perguntas: converse sobre o dia da pessoa)" : questions);
  }

  private static String hint(TipoResposta type) {
    return switch (type) {
      case YES_NO -> "resposta sim ou não";
      case SCALE_1_5 -> "peça uma nota de 1 a 5";
      case FREE_TEXT -> "resposta livre";
    };
  }
}
