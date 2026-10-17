package br.com.cuida.call.mock;

import br.com.cuida.call.OutboundCall;
import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import java.util.Map;

/** Gera a transcrição simulada (no formato do Vapi: linhas "AI:" e "User:"). */
final class MockConversation {

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

  private MockConversation() {}

  static String transcript(OutboundCall call, MockScenario scenario) {
    StringBuilder text = new StringBuilder();
    line(text, "AI", call.firstMessage());
    line(text, "User", "Oi, pode falar sim, minha filha.");
    for (QuestionnaireSnapshot.Item item : call.questionnaire().items()) {
      line(text, "AI", item.questionText());
      String answer =
          scenario == MockScenario.URGENT && item.attributeKey().equals("pain")
              ? URGENT_PAIN
              : ANSWERS.getOrDefault(item.attributeKey(), "Sim, está tudo certo.");
      line(text, "User", answer);
    }
    if (scenario == MockScenario.URGENT) {
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
