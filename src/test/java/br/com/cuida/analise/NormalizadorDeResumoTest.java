package br.com.cuida.analise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.Humor;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusResposta;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import br.com.cuida.questionario.model.TipoResposta;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;

class NormalizadorDeResumoTest {

  private static final QuestionarioSnapshot QUESTIONS =
      new QuestionarioSnapshot(
          List.of(
              new QuestionarioSnapshot.Item("sleep", "Dormiu bem?", TipoResposta.YES_NO, 0),
              new QuestionarioSnapshot.Item("pain", "Sente dor?", TipoResposta.SCALE_1_5, 1),
              new QuestionarioSnapshot.Item(
                  "message_for_family", "Quer deixar um recado?", TipoResposta.FREE_TEXT, 2)));

  private final NormalizadorDeResumo normalizer =
      new NormalizadorDeResumo(Validation.buildDefaultValidatorFactory().getValidator());

  @Test
  void keepsOneAnswerPerQuestionInOrderAndDropsInventedKeys() {
    ResumoLigacao raw =
        summary(
            List.of(
                answer("pain", "Não", StatusResposta.OK, "Sem dor"),
                answer("weather", "Sol", StatusResposta.OK, "Dia de sol")),
            NivelAlerta.NONE,
            List.of("motivo que não deveria ficar"),
            null);

    ResumoLigacao normalized = normalizer.normalize(raw, QUESTIONS, "Idoso(a): tudo bem");

    assertThat(normalized.answers())
        .extracting(RespostaAtributo::attributeKey)
        .containsExactly("sleep", "pain", "message_for_family");
    assertThat(normalized.answers().getFirst().clarity()).isEqualTo(ClarezaResposta.NOT_ASKED);
    assertThat(normalized.answers().getFirst().status()).isEqualTo(StatusResposta.UNKNOWN);
    assertThat(normalized.answers().get(1).question()).isEqualTo("Sente dor?");
    assertThat(normalized.answers().get(1).headline()).isEqualTo("Sem dor");
    assertThat(normalized.alertReasons()).isEmpty();
    assertThat(normalized.followUpSuggested()).isFalse();
  }

  @Test
  void missingStatusBecomesUnknownInsteadOfOk() {
    ResumoLigacao raw =
        summary(
            List.of(answer("sleep", "Dormi bem", null, null)), NivelAlerta.NONE, List.of(), null);

    ResumoLigacao normalized = normalizer.normalize(raw, QUESTIONS, "");

    assertThat(normalized.answers().getFirst().status()).isEqualTo(StatusResposta.UNKNOWN);
    assertThat(normalized.answers().getFirst().headline()).isNull();
  }

  @Test
  void familyMessageFallsBackToTheAnswerOfTheMessageQuestion() {
    ResumoLigacao raw =
        summary(
            List.of(
                answer(
                    "message_for_family",
                    "Vou ao médico na segunda-feira.",
                    StatusResposta.OK,
                    "Deixou um recado")),
            NivelAlerta.NONE,
            List.of(),
            "  ");

    assertThat(normalizer.normalize(raw, QUESTIONS, "").familyMessage())
        .isEqualTo("Vou ao médico na segunda-feira.");
  }

  @Test
  void negativeAnswerToTheMessageQuestionIsNotAMessage() {
    ResumoLigacao raw =
        summary(
            List.of(
                answer("message_for_family", "Não, obrigada.", StatusResposta.OK, "Sem recado")),
            NivelAlerta.NONE,
            List.of(),
            null);

    assertThat(normalizer.normalize(raw, QUESTIONS, "").familyMessage()).isNull();
  }

  @Test
  void redFlagInElderlySpeechRaisesNoneToAttention() {
    ResumoLigacao normalized =
        normalizer.normalize(
            summary(List.of(), NivelAlerta.NONE, List.of(), null),
            QUESTIONS,
            "Assistente: Tudo bem?\nIdoso(a): Ontem eu caí na cozinha.");

    assertThat(normalized.alertLevel()).isEqualTo(NivelAlerta.ATTENTION);
    assertThat(normalized.alertReasons()).singleElement().asString().contains("queda");
    assertThat(normalized.followUpSuggested()).isTrue();
  }

  @Test
  void scamMentionRaisesNoneToAttention() {
    ResumoLigacao normalized =
        normalizer.normalize(
            summary(List.of(), NivelAlerta.NONE, List.of(), null),
            QUESTIONS,
            "Idoso(a): Ligaram dizendo que meu neto estava preso e pediram dinheiro no pix.");

    assertThat(normalized.alertLevel()).isEqualTo(NivelAlerta.ATTENTION);
    assertThat(normalized.alertReasons()).singleElement().asString().contains("golpe");
  }

  @Test
  void assistantSpeechDoesNotTriggerTheSafetyNet() {
    ResumoLigacao normalized =
        normalizer.normalize(
            summary(List.of(), NivelAlerta.NONE, List.of(), null),
            QUESTIONS,
            "Assistente: Nunca passe sua senha nem faça pix para estranhos.\nIdoso(a): Tá bom.");

    assertThat(normalized.alertLevel()).isEqualTo(NivelAlerta.NONE);
  }

  @Test
  void blankOverviewIsInvalid() {
    ResumoLigacao raw =
        new ResumoLigacao(
            " ", Humor.GOOD, List.of(), List.of(), NivelAlerta.NONE, List.of(), false, null);

    assertThatThrownBy(() -> normalizer.normalize(raw, QUESTIONS, ""))
        .isInstanceOf(ResumoInvalidoException.class)
        .hasMessageContaining("overview");
  }

  private static RespostaAtributo answer(
      String key, String text, StatusResposta status, String headline) {
    return new RespostaAtributo(key, "?", text, ClarezaResposta.CLEAR, status, headline);
  }

  private static ResumoLigacao summary(
      List<RespostaAtributo> answers, NivelAlerta level, List<String> reasons, String message) {
    return new ResumoLigacao(
        "Está bem.", Humor.GOOD, answers, List.of(), level, reasons, false, message);
  }
}
