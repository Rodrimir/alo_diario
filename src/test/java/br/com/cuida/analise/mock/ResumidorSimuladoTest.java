package br.com.cuida.analise.mock;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.analise.model.PedidoDeResumo;
import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import br.com.cuida.questionario.model.TipoResposta;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumidorSimuladoTest {

  private static final QuestionarioSnapshot QUESTIONS =
      new QuestionarioSnapshot(
          List.of(
              new QuestionarioSnapshot.Item("sleep", "Dormiu bem?", TipoResposta.YES_NO, 0),
              new QuestionarioSnapshot.Item("pain", "Sente dor?", TipoResposta.SCALE_1_5, 1)));

  @Test
  void takesTheAnswerRightAfterEachQuestion() {
    ResumoLigacao summary =
        new ResumidorSimulado()
            .summarize(
                new PedidoDeResumo(
                    QUESTIONS,
                    """
                    Assistente: Dormiu bem?
                    Idoso(a): Dormi bem, graças a Deus.
                    Assistente: Até amanhã.""",
                    LocalDate.of(2026, 10, 17)));

    assertThat(summary.answers().getFirst().answer()).isEqualTo("Dormi bem, graças a Deus.");
    assertThat(summary.answers().get(1).clarity()).isEqualTo(ClarezaResposta.NOT_ASKED);
    assertThat(summary.alertLevel()).isEqualTo(NivelAlerta.NONE);
    assertThat(summary.overview()).startsWith("Resumo simulado");
  }

  @Test
  void fallReportIsUrgent() {
    ResumoLigacao summary =
        new ResumidorSimulado()
            .summarize(
                new PedidoDeResumo(
                    QUESTIONS,
                    "Assistente: Sente dor?\nIdoso(a): Eu caí e estou com dor forte no quadril.",
                    LocalDate.of(2026, 10, 17)));

    assertThat(summary.alertLevel()).isEqualTo(NivelAlerta.URGENT);
    assertThat(summary.alertReasons()).isNotEmpty();
    assertThat(summary.followUpSuggested()).isTrue();
  }
}
