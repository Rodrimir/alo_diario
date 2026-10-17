package br.com.cuida.analysis.mock;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.analysis.SummaryRequest;
import br.com.cuida.call.AlertLevel;
import br.com.cuida.call.AnswerClarity;
import br.com.cuida.call.CallSummary;
import br.com.cuida.questionnaire.AnswerType;
import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockSummarizerTest {

  private static final QuestionnaireSnapshot QUESTIONS =
      new QuestionnaireSnapshot(
          List.of(
              new QuestionnaireSnapshot.Item("sleep", "Dormiu bem?", AnswerType.YES_NO, 0),
              new QuestionnaireSnapshot.Item("pain", "Sente dor?", AnswerType.SCALE_1_5, 1)));

  @Test
  void takesTheAnswerRightAfterEachQuestion() {
    CallSummary summary =
        new MockSummarizer()
            .summarize(
                new SummaryRequest(
                    QUESTIONS,
                    """
                    Assistente: Dormiu bem?
                    Idoso(a): Dormi bem, graças a Deus.
                    Assistente: Até amanhã.""",
                    LocalDate.of(2026, 10, 17)));

    assertThat(summary.answers().getFirst().answer()).isEqualTo("Dormi bem, graças a Deus.");
    assertThat(summary.answers().get(1).clarity()).isEqualTo(AnswerClarity.NOT_ASKED);
    assertThat(summary.alertLevel()).isEqualTo(AlertLevel.NONE);
    assertThat(summary.overview()).startsWith("Resumo simulado");
  }

  @Test
  void fallReportIsUrgent() {
    CallSummary summary =
        new MockSummarizer()
            .summarize(
                new SummaryRequest(
                    QUESTIONS,
                    "Assistente: Sente dor?\nIdoso(a): Eu caí e estou com dor forte no quadril.",
                    LocalDate.of(2026, 10, 17)));

    assertThat(summary.alertLevel()).isEqualTo(AlertLevel.URGENT);
    assertThat(summary.alertReasons()).isNotEmpty();
    assertThat(summary.followUpSuggested()).isTrue();
  }
}
