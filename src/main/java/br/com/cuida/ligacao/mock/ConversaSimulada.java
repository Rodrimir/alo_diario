package br.com.cuida.ligacao.mock;

import br.com.cuida.ligacao.model.PedidoDeLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import java.util.Map;

/** Gera a transcrição simulada (no formato do Vapi: linhas "AI:" e "User:"). */
final class ConversaSimulada {

  private static final Map<String, String> ANSWERS =
      Map.of(
          "wellbeing", "Estou bem, só com um pouco de dor no joelho.",
          "medication", "Tomei sim, logo depois do café da manhã.",
          "sleep", "Dormi bem, acordei só uma vez.",
          "needed_help", "Não, não precisei não.",
          "suspicious_call", "Não, ninguém estranho ligou.",
          "message_for_family", "Diga para minha filha que vou ao médico na segunda-feira.");

  private static final String URGENT_WELLBEING =
      "Ai, ontem à noite eu caí no banheiro e estou com uma dor forte no quadril.";

  private static final String URGENT_HELP = "Precisei sim, a vizinha teve que me levantar.";

  private ConversaSimulada() {}

  static String transcript(PedidoDeLigacao call, CenarioSimulado scenario) {
    StringBuilder text = new StringBuilder();
    line(text, "AI", call.firstMessage());
    line(text, "User", "Oi, pode falar sim, minha filha.");
    for (QuestionarioSnapshot.Item item : call.questionnaire().items()) {
      line(text, "AI", item.questionText());
      String answer =
          scenario == CenarioSimulado.URGENT && item.attributeKey().equals("wellbeing")
              ? URGENT_WELLBEING
              : scenario == CenarioSimulado.URGENT && item.attributeKey().equals("needed_help")
                  ? URGENT_HELP
                  : ANSWERS.getOrDefault(item.attributeKey(), "Sim, está tudo certo.");
      line(text, "User", answer);
    }
    if (scenario == CenarioSimulado.URGENT) {
      line(
          text,
          "AI",
          "Sinto muito. Se a dor estiver forte, ligue para o SAMU no 192 ou chame alguém"
              + " próximo. Sua família vai ver o seu relato.");
      line(text, "User", "Tá bom, vou pedir para a vizinha me ajudar.");
    }
    line(text, "AI", "Obrigada pela conversa! Tenha um ótimo dia, até amanhã.");
    line(text, "User", "Tchau, até amanhã.");
    return text.toString().trim();
  }

  private static void line(StringBuilder text, String speaker, String content) {
    text.append(speaker).append(": ").append(content).append('\n');
  }
}
