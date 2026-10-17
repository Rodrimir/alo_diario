package br.com.cuida.analysis.mock;

import br.com.cuida.analysis.Summarizer;
import br.com.cuida.analysis.SummaryRequest;
import br.com.cuida.call.AlertLevel;
import br.com.cuida.call.AnswerClarity;
import br.com.cuida.call.AttributeAnswer;
import br.com.cuida.call.CallSummary;
import br.com.cuida.call.Mood;
import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Resumo simulado e determinístico (sem chave de API), feito por regras simples sobre a
 * transcrição: a resposta de cada pergunta é a fala da pessoa logo depois dela. Serve para o dev e
 * os testes exercitarem o fluxo inteiro; o texto deixa claro que é simulado.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
class MockSummarizer implements Summarizer {

  /** Termos buscados como palavra inteira; terminados em {@code *}, como prefixo. */
  private static final List<String> URGENT = List.of("caí", "dor forte", "falta de ar", "socorro");

  private static final List<String> ATTENTION = List.of("dor", "triste", "não tomei", "sozinh*");
  private static final List<String> MENTIONS = List.of("net*", "filh*", "visit*", "médic*");

  @Override
  public CallSummary summarize(SummaryRequest request) {
    List<String> lines = request.transcript().lines().map(String::trim).toList();
    List<String> speech =
        lines.stream()
            .filter(line -> line.startsWith("Idoso(a):"))
            .map(line -> line.substring("Idoso(a):".length()).trim())
            .toList();
    String allSpeech = String.join(" ", speech).toLowerCase(Locale.ROOT);

    List<AttributeAnswer> answers = new ArrayList<>();
    int answered = 0;
    for (QuestionnaireSnapshot.Item item : request.questionnaire().items()) {
      String answer = answerAfter(lines, item.questionText());
      if (answer != null) {
        answered++;
      }
      answers.add(
          new AttributeAnswer(
              item.attributeKey(),
              item.questionText(),
              answer == null ? "" : answer,
              answer == null ? AnswerClarity.NOT_ASKED : AnswerClarity.CLEAR));
    }

    AlertLevel level =
        containsAny(allSpeech, URGENT)
            ? AlertLevel.URGENT
            : containsAny(allSpeech, ATTENTION) ? AlertLevel.ATTENTION : AlertLevel.NONE;
    List<String> reasons =
        level == AlertLevel.NONE
            ? List.of()
            : speech.stream()
                .filter(
                    s ->
                        containsAny(
                            s.toLowerCase(Locale.ROOT),
                            level == AlertLevel.URGENT ? URGENT : ATTENTION))
                .limit(3)
                .toList();
    Mood mood =
        containsAny(allSpeech, List.of("triste", "sozinh*"))
            ? Mood.LOW
            : containsAny(allSpeech, List.of("animad*", "bem")) ? Mood.GOOD : Mood.NEUTRAL;
    List<String> mentions =
        speech.stream()
            .filter(s -> containsAny(s.toLowerCase(Locale.ROOT), MENTIONS))
            .limit(5)
            .toList();

    String overview =
        "Resumo simulado: ligação concluída, "
            + answered
            + " de "
            + answers.size()
            + " perguntas respondidas."
            + (level == AlertLevel.URGENT
                ? " A pessoa relatou algo que exige atenção imediata."
                : level == AlertLevel.ATTENTION ? " Há pontos de atenção." : " Nada preocupante.");
    return new CallSummary(
        overview, mood, answers, mentions, level, reasons, level != AlertLevel.NONE);
  }

  private static String answerAfter(List<String> lines, String question) {
    String needle = question.toLowerCase(Locale.ROOT);
    for (int i = 0; i < lines.size() - 1; i++) {
      if (lines.get(i).toLowerCase(Locale.ROOT).contains(needle)
          && lines.get(i + 1).startsWith("Idoso(a):")) {
        return lines.get(i + 1).substring("Idoso(a):".length()).trim();
      }
    }
    return null;
  }

  private static boolean containsAny(String text, List<String> terms) {
    return terms.stream().anyMatch(term -> pattern(term).matcher(text).find());
  }

  private static Pattern pattern(String term) {
    boolean prefix = term.endsWith("*");
    String word = Pattern.quote(prefix ? term.substring(0, term.length() - 1) : term);
    return Pattern.compile("(?<!\\p{L})" + word + (prefix ? "" : "(?!\\p{L})"));
  }
}
