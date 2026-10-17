package br.com.cuida.analise;

import br.com.cuida.ligacao.FormatadorDeResumo;
import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.Humor;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusResposta;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Deixa o resumo do modelo consistente com o questionário e com as regras do produto:
 *
 * <ul>
 *   <li>exatamente uma resposta por pergunta do snapshot, na mesma ordem (as que faltarem viram
 *       {@code NOT_ASKED}; chaves inventadas são descartadas);
 *   <li>{@code alertReasons} vazio quando o nível é {@code NONE};
 *   <li>rede de segurança: se a fala da pessoa menciona queda, dor no peito, falta de ar ou pedido
 *       de ajuda ou golpe e o modelo disse {@code NONE}, sobe para {@code ATTENTION} pedindo
 *       confirmação;
 *   <li>status do checklist ausente vira {@code UNKNOWN}; o recado vem do modelo ou da resposta à
 *       pergunta {@code message_for_family}.
 * </ul>
 *
 * Depois valida com Jakarta Validation; resumo inválido lança {@link ResumoInvalidoException}.
 */
@Component
class NormalizadorDeResumo {

  private static final Map<String, String> RED_FLAGS =
      Map.ofEntries(
          Map.entry("cai ", "queda"),
          Map.entry("caí", "queda"),
          Map.entry("queda", "queda"),
          Map.entry("dor no peito", "dor no peito"),
          Map.entry("falta de ar", "falta de ar"),
          Map.entry("socorro", "pedido de ajuda"),
          Map.entry("me ajuda", "pedido de ajuda"),
          Map.entry("golpe", "possível tentativa de golpe"),
          Map.entry(" pix", "possível tentativa de golpe"),
          Map.entry("senha", "possível tentativa de golpe"),
          Map.entry("pediram dinheiro", "possível tentativa de golpe"),
          Map.entry("pedindo dinheiro", "possível tentativa de golpe"),
          Map.entry("sequestr", "possível tentativa de golpe"));

  private final Validator validator;

  NormalizadorDeResumo(Validator validator) {
    this.validator = validator;
  }

  ResumoLigacao normalize(
      ResumoLigacao raw, QuestionarioSnapshot questionnaire, String transcript) {
    Map<String, RespostaAtributo> byKey = new LinkedHashMap<>();
    for (RespostaAtributo answer :
        raw.answers() == null ? List.<RespostaAtributo>of() : raw.answers()) {
      if (answer != null && answer.attributeKey() != null) {
        byKey.putIfAbsent(answer.attributeKey(), answer);
      }
    }
    List<RespostaAtributo> answers = new ArrayList<>();
    for (QuestionarioSnapshot.Item item : questionnaire.items()) {
      RespostaAtributo answer = byKey.get(item.attributeKey());
      answers.add(answer == null ? notAsked(item) : normalize(answer, item));
    }

    NivelAlerta level = raw.alertLevel() == null ? NivelAlerta.NONE : raw.alertLevel();
    List<String> reasons = clean(raw.alertReasons());
    Set<String> flags = redFlags(transcript);
    if (level == NivelAlerta.NONE && !flags.isEmpty()) {
      level = NivelAlerta.ATTENTION;
      reasons =
          List.of(
              "Possível menção a "
                  + String.join(", ", flags)
                  + " na conversa; confirme com a pessoa.");
    }
    if (level == NivelAlerta.NONE) {
      reasons = List.of();
    }

    ResumoLigacao normalized =
        new ResumoLigacao(
            truncate(raw.overview() == null ? "" : raw.overview().trim(), 800),
            raw.mood() == null ? Humor.UNCLEAR : raw.mood(),
            answers,
            clean(raw.notableMentions()),
            level,
            reasons,
            raw.followUpSuggested() || level != NivelAlerta.NONE,
            familyMessage(raw, answers));
    Set<ConstraintViolation<ResumoLigacao>> violations = validator.validate(normalized);
    if (!violations.isEmpty()) {
      throw new ResumoInvalidoException(
          violations.stream()
              .map(v -> v.getPropertyPath() + " " + v.getMessage())
              .collect(Collectors.joining("; ")));
    }
    return normalized;
  }

  private static RespostaAtributo notAsked(QuestionarioSnapshot.Item item) {
    return new RespostaAtributo(
        item.attributeKey(),
        item.questionText(),
        "",
        ClarezaResposta.NOT_ASKED,
        StatusResposta.UNKNOWN,
        null);
  }

  /**
   * Sem status vindo do modelo, a resposta fica {@code UNKNOWN} (❔): melhor mostrar "sem
   * informação" do que um ✅ que ninguém confirmou.
   */
  private static RespostaAtributo normalize(
      RespostaAtributo answer, QuestionarioSnapshot.Item item) {
    String text = truncate(answer.answer() == null ? "" : answer.answer().trim(), 500);
    ClarezaResposta clarity = answer.clarity() == null ? ClarezaResposta.PARTIAL : answer.clarity();
    boolean unanswered =
        text.isEmpty()
            || clarity == ClarezaResposta.NOT_ASKED
            || clarity == ClarezaResposta.NOT_ANSWERED;
    StatusResposta status =
        unanswered || answer.status() == null ? StatusResposta.UNKNOWN : answer.status();
    String headline =
        answer.headline() == null || answer.headline().isBlank()
            ? null
            : truncate(answer.headline().trim(), 80);
    return new RespostaAtributo(
        item.attributeKey(), item.questionText(), text, clarity, status, headline);
  }

  /**
   * O recado vem do modelo; se ele não preencheu, usa a resposta à pergunta do recado, a menos que
   * a pessoa tenha dito que não quer deixar recado.
   */
  private static String familyMessage(ResumoLigacao raw, List<RespostaAtributo> answers) {
    if (raw.familyMessage() != null && !raw.familyMessage().isBlank()) {
      return truncate(raw.familyMessage().trim(), 500);
    }
    return answers.stream()
        .filter(a -> FormatadorDeResumo.FAMILY_MESSAGE_KEY.equals(a.attributeKey()))
        .filter(a -> a.statusOrUnknown() != StatusResposta.UNKNOWN)
        .map(RespostaAtributo::answer)
        .filter(text -> !text.toLowerCase(Locale.ROOT).startsWith("não"))
        .findFirst()
        .orElse(null);
  }

  /** Procura sinais de alerta só nas falas da pessoa idosa. */
  static Set<String> redFlags(String transcript) {
    if (transcript == null) {
      return Set.of();
    }
    String elderlySpeech =
        transcript
            .lines()
            .filter(line -> line.startsWith("Idoso(a):"))
            .map(line -> " " + line.toLowerCase(Locale.ROOT) + " ")
            .collect(Collectors.joining("\n"));
    String withoutAccents =
        Normalizer.normalize(elderlySpeech, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    Set<String> found = new TreeSet<>();
    RED_FLAGS.forEach(
        (term, label) -> {
          if (elderlySpeech.contains(term) || withoutAccents.contains(term)) {
            found.add(label);
          }
        });
    return found;
  }

  private static List<String> clean(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .filter(v -> v != null && !v.isBlank())
        .map(v -> truncate(v.trim(), 300))
        .limit(10)
        .toList();
  }

  private static String truncate(String value, int max) {
    return value.length() > max ? value.substring(0, max) : value;
  }
}
