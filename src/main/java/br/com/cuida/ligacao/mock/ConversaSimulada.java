package br.com.cuida.ligacao.mock;

import br.com.cuida.ligacao.model.PedidoDeLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import java.util.Map;

/** Gera a transcrição simulada (no formato do Vapi: linhas "AI:" e "User:"). */
final class ConversaSimulada {

  private static final Map<String, String> ANSWERS =
      Map.of(
          "sleep", "Dormi bem, acordei só uma vez para ir ao banheiro.",
          "medication", "Tomei sim, logo depois do café da manhã.",
          "meals", "Comi pão com manteiga, café com leite e uma banana.",
          "pain", "Só um pouquinho de dor no joelho, uns 2.",
          "mood", "Estou animada hoje, o dia está bonito.",
          "social_contact", "Minha neta vem me visitar à tarde.");

  private static final String URGENT_PAIN =
      "Ai, ontem à noite eu caí no banheiro e estou com uma dor forte no quadril, uns 5.";

  private ConversaSimulada() {}

  static String transcript(PedidoDeLigacao call, CenarioSimulado scenario) {
    StringBuilder text = new StringBuilder();
    line(text, "AI", call.firstMessage());
    line(text, "User", "Oi, pode falar sim, minha filha.");
    for (QuestionarioSnapshot.Item item : call.questionnaire().items()) {
      line(text, "AI", item.questionText());
      String answer =
          scenario == CenarioSimulado.URGENT && item.attributeKey().equals("pain")
              ? URGENT_PAIN
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
