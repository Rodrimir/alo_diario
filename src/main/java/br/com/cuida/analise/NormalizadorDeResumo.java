package br.com.cuida.analise;

import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.Humor;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoLigacao;
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
 *       de ajuda e o modelo disse {@code NONE}, sobe para {@code ATTENTION} pedindo confirmação.
 * </ul>
 *
 * Depois valida com Jakarta Validation; resumo inválido lança {@link ResumoInvalidoException}.
 */
@Component
class NormalizadorDeResumo {

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
      answers.add(
          answer == null
              ? new RespostaAtributo(
                  item.attributeKey(), item.questionText(), "", ClarezaResposta.NOT_ASKED)
              : new RespostaAtributo(
                  item.attributeKey(),
                  item.questionText(),
                  truncate(answer.answer() == null ? "" : answer.answer(), 500),
                  answer.clarity() == null ? ClarezaResposta.PARTIAL : answer.clarity()));
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
            raw.followUpSuggested() || level != NivelAlerta.NONE);
    Set<ConstraintViolation<ResumoLigacao>> violations = validator.validate(normalized);
    if (!violations.isEmpty()) {
      throw new ResumoInvalidoException(
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
