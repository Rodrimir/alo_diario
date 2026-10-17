package br.com.cuida.analise.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import br.com.cuida.analise.model.PedidoDeResumo;
import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusResposta;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import br.com.cuida.questionario.model.TipoResposta;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumidorSimuladoTest {

  private static final QuestionarioSnapshot QUESTIONS =
      new QuestionarioSnapshot(
          List.of(
              new QuestionarioSnapshot.Item(
                  "medication", "Tomou os remédios?", TipoResposta.YES_NO, 0),
              new QuestionarioSnapshot.Item(
                  "suspicious_call", "Recebeu ligação suspeita?", TipoResposta.YES_NO, 1),
              new QuestionarioSnapshot.Item(
                  "message_for_family", "Quer deixar um recado?", TipoResposta.FREE_TEXT, 2),
              new QuestionarioSnapshot.Item("sleep", "Dormiu bem?", TipoResposta.YES_NO, 3)));

  @Test
  void buildsThePitchChecklistFromTheAnswers() {
    ResumoLigacao summary =
        summarize(
            """
            Assistente: Tomou os remédios?
            Idoso(a): Não, esqueci hoje.
            Assistente: Recebeu ligação suspeita?
            Idoso(a): Não, ninguém ligou.
            Assistente: Quer deixar um recado?
            Idoso(a): Diga que amo todos.""");

    assertThat(summary.answers())
        .extracting(RespostaAtributo::status, RespostaAtributo::headline)
        .containsExactly(
            tuple(StatusResposta.ATTENTION, "Não tomou os medicamentos"),
            tuple(StatusResposta.OK, "Nenhuma tentativa de golpe"),
            tuple(StatusResposta.OK, "Deixou um recado"),
            tuple(StatusResposta.UNKNOWN, null));
    assertThat(summary.answers().get(3).clarity()).isEqualTo(ClarezaResposta.NOT_ASKED);
    assertThat(summary.familyMessage()).isEqualTo("Diga que amo todos.");
    assertThat(summary.overview()).startsWith("Resumo simulado");
  }

  @Test
  void fallReportIsUrgent() {
    ResumoLigacao summary =
        summarize("Assistente: Dormiu bem?\nIdoso(a): Eu caí e estou com dor forte no quadril.");

    assertThat(summary.alertLevel()).isEqualTo(NivelAlerta.URGENT);
    assertThat(summary.alertReasons()).isNotEmpty();
    assertThat(summary.followUpSuggested()).isTrue();
  }

  private static ResumoLigacao summarize(String transcript) {
    return new ResumidorSimulado()
        .summarize(new PedidoDeResumo(QUESTIONS, transcript, LocalDate.of(2026, 10, 17)));
  }
}
