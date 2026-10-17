package br.com.cuida.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.call.AlertLevel;
import br.com.cuida.call.AnswerClarity;
import br.com.cuida.call.AttributeAnswer;
import br.com.cuida.call.CallSummary;
import br.com.cuida.call.Mood;
import br.com.cuida.questionnaire.AnswerType;
import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryNormalizerTest {

  private static final QuestionnaireSnapshot QUESTIONS =
      new QuestionnaireSnapshot(
          List.of(
              new QuestionnaireSnapshot.Item("sleep", "Dormiu bem?", AnswerType.YES_NO, 0),
              new QuestionnaireSnapshot.Item("pain", "Sente dor?", AnswerType.SCALE_1_5, 1)));

  private final SummaryNormalizer normalizer =
      new SummaryNormalizer(Validation.buildDefaultValidatorFactory().getValidator());

  @Test
  void keepsOneAnswerPerQuestionInOrderAndDropsInventedKeys() {
    CallSummary raw =
        summary(
            List.of(
                new AttributeAnswer("pain", "?", "Não", AnswerClarity.CLEAR),
                new AttributeAnswer("weather", "Tempo?", "Sol", AnswerClarity.CLEAR)),
            AlertLevel.NONE,
            List.of("motivo que não deveria ficar"));

    CallSummary normalized = normalizer.normalize(raw, QUESTIONS, "Idoso(a): tudo bem");

    assertThat(normalized.answers())
        .extracting(AttributeAnswer::attributeKey)
        .containsExactly("sleep", "pain");
    assertThat(normalized.answers().getFirst().clarity()).isEqualTo(AnswerClarity.NOT_ASKED);
    assertThat(normalized.answers().get(1).question()).isEqualTo("Sente dor?");
    assertThat(normalized.alertReasons()).isEmpty();
    assertThat(normalized.followUpSuggested()).isFalse();
  }

  @Test
  void redFlagInElderlySpeechRaisesNoneToAttention() {
    CallSummary normalized =
        normalizer.normalize(
            summary(List.of(), AlertLevel.NONE, List.of()),
            QUESTIONS,
            "Assistente: Tudo bem?\nIdoso(a): Ontem eu caí na cozinha.");

    assertThat(normalized.alertLevel()).isEqualTo(AlertLevel.ATTENTION);
    assertThat(normalized.alertReasons()).singleElement().asString().contains("queda");
    assertThat(normalized.followUpSuggested()).isTrue();
  }

  @Test
  void assistantSpeechDoesNotTriggerTheSafetyNet() {
    CallSummary normalized =
        normalizer.normalize(
            summary(List.of(), AlertLevel.NONE, List.of()),
            QUESTIONS,
            "Assistente: Se sentir falta de ar, ligue 192.\nIdoso(a): Tá bom.");

    assertThat(normalized.alertLevel()).isEqualTo(AlertLevel.NONE);
  }

  @Test
  void blankOverviewIsInvalid() {
    CallSummary raw =
        new CallSummary(" ", Mood.GOOD, List.of(), List.of(), AlertLevel.NONE, List.of(), false);

    assertThatThrownBy(() -> normalizer.normalize(raw, QUESTIONS, ""))
        .isInstanceOf(InvalidSummaryException.class)
        .hasMessageContaining("overview");
  }

  private static CallSummary summary(
      List<AttributeAnswer> answers, AlertLevel level, List<String> reasons) {
    return new CallSummary("Está bem.", Mood.GOOD, answers, List.of(), level, reasons, false);
  }
}
