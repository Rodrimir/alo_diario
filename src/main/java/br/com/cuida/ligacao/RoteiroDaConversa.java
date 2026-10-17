package br.com.cuida.ligacao;

import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.PedidoDeLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import br.com.cuida.questionario.model.TipoResposta;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Monta a abertura e as instruções da assistente. A abertura é fixa (cumprimento conforme a hora no
 * fuso do idoso) e sempre avisa que a ligação é gravada, sem depender do LLM.
 */
@Component
class RoteiroDaConversa {

  private final LigacaoProperties ligacaoProperties;
  private final Clock clock;
  private final ObjectProvider<ProvedorDeContexto> provedorDeContexto;

  RoteiroDaConversa(
      LigacaoProperties ligacaoProperties,
      Clock clock,
      ObjectProvider<ProvedorDeContexto> provedorDeContexto) {
    this.ligacaoProperties = ligacaoProperties;
    this.clock = clock;
    this.provedorDeContexto = provedorDeContexto;
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
        instrucoes(call, elderly, questionnaire),
        questionnaire,
        ligacaoProperties.maxDurationSeconds());
  }

  /**
   * Com o módulo MCP presente, as instruções vêm das três camadas de contexto (regras base, skills
   * da pessoa e pedidos da família); sem ele, das instruções padrão abaixo.
   */
  private String instrucoes(Ligacao call, IdosoDTO elderly, QuestionarioSnapshot questionnaire) {
    ProvedorDeContexto provedor = provedorDeContexto.getIfAvailable();
    if (provedor == null) {
      return systemPrompt(elderly, questionnaire);
    }
    return provedor.montar(
        call.getId(), elderly, questionnaire, LocalDate.ofInstant(clock.instant(), elderly.zone()));
  }

  String firstMessage(IdosoDTO elderly) {
    return cumprimento(LocalTime.ofInstant(clock.instant(), elderly.zone()))
        + ", "
        + elderly.nameForConversation()
        + "! Aqui é a assistente virtual do "
        + ligacaoProperties.productName()
        + ". Esta ligação é gravada para que sua família acompanhe como você está. "
        + "Gostaria de saber como você está hoje.";
  }

  /** Bom dia (5h–11h59), Boa tarde (12h–17h59) ou Boa noite, no horário local do idoso. */
  static String cumprimento(LocalTime horario) {
    int hora = horario.getHour();
    if (hora >= 5 && hora < 12) {
      return "Bom dia";
    }
    return hora >= 12 && hora < 18 ? "Boa tarde" : "Boa noite";
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
        Você é a assistente virtual do %s, brasileira, calorosa e paciente. Liga uma vez por dia \
        para %s, uma pessoa idosa que mora sozinha, a pedido da família, para confirmar que está \
        tudo bem. Não é uma conversa longa nem companhia: é uma checagem rápida e gentil.

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
        - No fim, pergunte se a pessoa quer deixar um recado para a família.
        - Ao terminar, agradeça, despeça-se ("até amanhã") e encerre a ligação.
        - Nunca peça dados pessoais, senhas, documentos ou dinheiro.
        """
        .formatted(
            ligacaoProperties.productName(),
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
