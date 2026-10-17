package br.com.cuida.analise.mock;

import br.com.cuida.analise.Resumidor;
import br.com.cuida.analise.model.PedidoDeResumo;
import br.com.cuida.ligacao.FormatadorDeResumo;
import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.Humor;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusResposta;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
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
class ResumidorSimulado implements Resumidor {

  /** Termos buscados como palavra inteira; terminados em {@code *}, como prefixo. */
  private static final List<String> URGENT = List.of("caí", "dor forte", "falta de ar", "socorro");

  private static final List<String> ATTENTION = List.of("dor", "triste", "não tomei", "sozinh*");
  private static final List<String> MENTIONS = List.of("net*", "filh*", "visit*", "médic*");

  @Override
  public ResumoLigacao summarize(PedidoDeResumo request) {
    List<String> lines = request.transcript().lines().map(String::trim).toList();
    List<String> speech =
        lines.stream()
            .filter(line -> line.startsWith("Idoso(a):"))
            .map(line -> line.substring("Idoso(a):".length()).trim())
            .toList();
    String allSpeech = String.join(" ", speech).toLowerCase(Locale.ROOT);

    List<RespostaAtributo> answers = new ArrayList<>();
    int answered = 0;
    for (QuestionarioSnapshot.Item item : request.questionnaire().items()) {
      String answer = answerAfter(lines, item.questionText());
      if (answer != null) {
        answered++;
      }
      answers.add(checklistItem(item, answer));
    }
    String familyMessage =
        answers.stream()
            .filter(a -> FormatadorDeResumo.FAMILY_MESSAGE_KEY.equals(a.attributeKey()))
            .map(RespostaAtributo::answer)
            .filter(a -> !a.isBlank() && !negative(a))
            .findFirst()
            .orElse(null);

    NivelAlerta level =
        containsAny(allSpeech, URGENT)
            ? NivelAlerta.URGENT
            : containsAny(allSpeech, ATTENTION) ? NivelAlerta.ATTENTION : NivelAlerta.NONE;
    List<String> reasons =
        level == NivelAlerta.NONE
            ? List.of()
            : speech.stream()
                .filter(
                    s ->
                        containsAny(
                            s.toLowerCase(Locale.ROOT),
                            level == NivelAlerta.URGENT ? URGENT : ATTENTION))
                .limit(3)
                .toList();
    Humor mood =
        containsAny(allSpeech, List.of("triste", "sozinh*"))
            ? Humor.LOW
            : containsAny(allSpeech, List.of("animad*", "bem")) ? Humor.GOOD : Humor.NEUTRAL;
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
            + (level == NivelAlerta.URGENT
                ? " A pessoa relatou algo que exige atenção imediata."
                : level == NivelAlerta.ATTENTION ? " Há pontos de atenção." : " Nada preocupante.");
    return new ResumoLigacao(
        overview,
        mood,
        answers,
        mentions,
        level,
        reasons,
        level != NivelAlerta.NONE,
        familyMessage);
  }

  /**
   * Status e frase do checklist por regras simples: para as perguntas sugeridas, a resposta
   * negativa ou afirmativa decide o ✅/⚠️; para as demais, termos de atenção na fala.
   */
  private static RespostaAtributo checklistItem(QuestionarioSnapshot.Item item, String answer) {
    if (answer == null) {
      return new RespostaAtributo(
          item.attributeKey(),
          item.questionText(),
          "",
          ClarezaResposta.NOT_ASKED,
          StatusResposta.UNKNOWN,
          null);
    }
    String lower = answer.toLowerCase(Locale.ROOT);
    boolean no = negative(answer);
    boolean worrying = containsAny(lower, URGENT) || containsAny(lower, ATTENTION);
    StatusResposta status;
    String headline;
    switch (item.attributeKey()) {
      case "wellbeing" -> {
        status = worrying ? StatusResposta.ATTENTION : StatusResposta.OK;
        headline = worrying ? "Relatou: " + answer : "Está bem";
      }
      case "medication" -> {
        status = no ? StatusResposta.ATTENTION : StatusResposta.OK;
        headline = no ? "Não tomou os medicamentos" : "Tomou os medicamentos";
      }
      case "sleep" -> {
        status = no ? StatusResposta.ATTENTION : StatusResposta.OK;
        headline = no ? "Não dormiu bem" : "Dormiu bem";
      }
      case "needed_help" -> {
        status = no ? StatusResposta.OK : StatusResposta.ATTENTION;
        headline = no ? "Não precisou de ajuda" : "Precisou de ajuda: " + answer;
      }
      case "suspicious_call" -> {
        status = no ? StatusResposta.OK : StatusResposta.ATTENTION;
        headline = no ? "Nenhuma tentativa de golpe" : "Recebeu contato suspeito: " + answer;
      }
      case FormatadorDeResumo.FAMILY_MESSAGE_KEY -> {
        status = StatusResposta.OK;
        headline = no ? "Sem recado" : "Deixou um recado";
      }
      default -> {
        status = worrying ? StatusResposta.ATTENTION : StatusResposta.OK;
        headline = answer;
      }
    }
    return new RespostaAtributo(
        item.attributeKey(),
        item.questionText(),
        answer,
        ClarezaResposta.CLEAR,
        status,
        headline.length() > 80 ? headline.substring(0, 77) + "..." : headline);
  }

  private static boolean negative(String answer) {
    String lower = answer.trim().toLowerCase(Locale.ROOT);
    return lower.startsWith("não") || lower.startsWith("nao") || lower.startsWith("nunca");
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
