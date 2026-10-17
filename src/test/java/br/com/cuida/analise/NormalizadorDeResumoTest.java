package br.com.cuida.analise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.Humor;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoLigacao;
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
              new QuestionarioSnapshot.Item("pain", "Sente dor?", TipoResposta.SCALE_1_5, 1)));

  private final NormalizadorDeResumo normalizer =
      new NormalizadorDeResumo(Validation.buildDefaultValidatorFactory().getValidator());

  @Test
  void keepsOneAnswerPerQuestionInOrderAndDropsInventedKeys() {
    ResumoLigacao raw =
        summary(
            List.of(
                new RespostaAtributo("pain", "?", "Não", ClarezaResposta.CLEAR),
                new RespostaAtributo("weather", "Tempo?", "Sol", ClarezaResposta.CLEAR)),
            NivelAlerta.NONE,
            List.of("motivo que não deveria ficar"));

    ResumoLigacao normalized = normalizer.normalize(raw, QUESTIONS, "Idoso(a): tudo bem");

    assertThat(normalized.answers())
        .extracting(RespostaAtributo::attributeKey)
        .containsExactly("sleep", "pain");
    assertThat(normalized.answers().getFirst().clarity()).isEqualTo(ClarezaResposta.NOT_ASKED);
    assertThat(normalized.answers().get(1).question()).isEqualTo("Sente dor?");
    assertThat(normalized.alertReasons()).isEmpty();
    assertThat(normalized.followUpSuggested()).isFalse();
  }

  @Test
  void redFlagInElderlySpeechRaisesNoneToAttention() {
    ResumoLigacao normalized =
        normalizer.normalize(
            summary(List.of(), NivelAlerta.NONE, List.of()),
            QUESTIONS,
            "Assistente: Tudo bem?\nIdoso(a): Ontem eu caí na cozinha.");

    assertThat(normalized.alertLevel()).isEqualTo(NivelAlerta.ATTENTION);
    assertThat(normalized.alertReasons()).singleElement().asString().contains("queda");
    assertThat(normalized.followUpSuggested()).isTrue();
  }

  @Test
  void assistantSpeechDoesNotTriggerTheSafetyNet() {
    ResumoLigacao normalized =
        normalizer.normalize(
            summary(List.of(), NivelAlerta.NONE, List.of()),
            QUESTIONS,
            "Assistente: Se sentir falta de ar, ligue 192.\nIdoso(a): Tá bom.");

    assertThat(normalized.alertLevel()).isEqualTo(NivelAlerta.NONE);
  }

  @Test
  void blankOverviewIsInvalid() {
    ResumoLigacao raw =
        new ResumoLigacao(
            " ", Humor.GOOD, List.of(), List.of(), NivelAlerta.NONE, List.of(), false);

    assertThatThrownBy(() -> normalizer.normalize(raw, QUESTIONS, ""))
        .isInstanceOf(ResumoInvalidoException.class)
        .hasMessageContaining("overview");
  }

  private static ResumoLigacao summary(
      List<RespostaAtributo> answers, NivelAlerta level, List<String> reasons) {
    return new ResumoLigacao("Está bem.", Humor.GOOD, answers, List.of(), level, reasons, false);
  }
}
