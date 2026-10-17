package br.com.cuida.analysis;

import br.com.cuida.call.AlertLevel;
import br.com.cuida.call.AnswerClarity;
import br.com.cuida.call.AttributeAnswer;
import br.com.cuida.call.CallSummary;
import br.com.cuida.call.Mood;
import br.com.cuida.questionnaire.QuestionnaireSnapshot;
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
 *       de ajuda e o modelo disse {@code NONE}, sobe para {@code ATTENTION} pedindo confirmação.
 * </ul>
 *
 * Depois valida com Jakarta Validation; resumo inválido lança {@link InvalidSummaryException}.
 */
@Component
class SummaryNormalizer {

  private static final Map<String, String> RED_FLAGS =
      Map.of(
          "cai ", "queda",
          "caí", "queda",
          "queda", "queda",
          "dor no peito", "dor no peito",
          "falta de ar", "falta de ar",
          "socorro", "pedido de ajuda",
          "me ajuda", "pedido de ajuda");

  private final Validator validator;

  SummaryNormalizer(Validator validator) {
    this.validator = validator;
  }

  CallSummary normalize(CallSummary raw, QuestionnaireSnapshot questionnaire, String transcript) {
    Map<String, AttributeAnswer> byKey = new LinkedHashMap<>();
    for (AttributeAnswer answer :
        raw.answers() == null ? List.<AttributeAnswer>of() : raw.answers()) {
      if (answer != null && answer.attributeKey() != null) {
        byKey.putIfAbsent(answer.attributeKey(), answer);
      }
    }
    List<AttributeAnswer> answers = new ArrayList<>();
    for (QuestionnaireSnapshot.Item item : questionnaire.items()) {
      AttributeAnswer answer = byKey.get(item.attributeKey());
      answers.add(
          answer == null
              ? new AttributeAnswer(
                  item.attributeKey(), item.questionText(), "", AnswerClarity.NOT_ASKED)
              : new AttributeAnswer(
                  item.attributeKey(),
                  item.questionText(),
                  truncate(answer.answer() == null ? "" : answer.answer(), 500),
                  answer.clarity() == null ? AnswerClarity.PARTIAL : answer.clarity()));
    }

    AlertLevel level = raw.alertLevel() == null ? AlertLevel.NONE : raw.alertLevel();
    List<String> reasons = clean(raw.alertReasons());
    Set<String> flags = redFlags(transcript);
    if (level == AlertLevel.NONE && !flags.isEmpty()) {
      level = AlertLevel.ATTENTION;
      reasons =
          List.of(
              "Possível menção a "
                  + String.join(", ", flags)
                  + " na conversa; confirme com a pessoa.");
    }
    if (level == AlertLevel.NONE) {
      reasons = List.of();
    }

    CallSummary normalized =
        new CallSummary(
            truncate(raw.overview() == null ? "" : raw.overview().trim(), 800),
            raw.mood() == null ? Mood.UNCLEAR : raw.mood(),
            answers,
            clean(raw.notableMentions()),
            level,
            reasons,
            raw.followUpSuggested() || level != AlertLevel.NONE);
    Set<ConstraintViolation<CallSummary>> violations = validator.validate(normalized);
    if (!violations.isEmpty()) {
      throw new InvalidSummaryException(
          violations.stream()
              .map(v -> v.getPropertyPath() + " " + v.getMessage())
              .collect(Collectors.joining("; ")));
    }
    return normalized;
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
