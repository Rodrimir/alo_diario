package br.com.cuida.ligacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.Humor;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoFormatadoDTO;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusResposta;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormatadorDeResumoTest {

  private final FormatadorDeResumo formatador =
      new FormatadorDeResumo(
          new LigacaoProperties(Duration.ofHours(2), Duration.ofMinutes(30), 600, "Alô Diário"));

  @Test
  void formatsTheReportLikeThePitch() {
    ResumoFormatadoDTO formatted =
        formatador.formatar(
            summary(NivelAlerta.ATTENTION, List.of()), "Dona Maria", LocalDate.of(2026, 10, 17));

    assertThat(formatted.report())
        .isEqualTo(
            """
            ALÔ DIÁRIO – Dona Maria
            Data: 17/10
            ✅ Está bem
            ✅ Tomou os medicamentos
            ⚠️ Relatou dor no joelho
            ❔ Dormiu bem? (sem resposta)
            Recado: "Vou ao médico na segunda-feira.\"""");
    assertThat(formatted.checklist()).hasSize(4);
    assertThat(formatted.familyMessage()).isEqualTo("Vou ao médico na segunda-feira.");
  }

  @Test
  void urgentAlertGoesOnTop() {
    ResumoFormatadoDTO formatted =
        formatador.formatar(
            summary(NivelAlerta.URGENT, List.of("Relatou uma queda no banheiro")),
            "Seu Zé",
            LocalDate.of(2026, 1, 5));

    assertThat(formatted.report().lines().toList().get(2))
        .isEqualTo("🚨 Atenção: Relatou uma queda no banheiro");
  }

  @Test
  void checklistInOneLineHasNoLineBreaks() {
    String line = formatador.checklistEmUmaLinha(summary(NivelAlerta.NONE, List.of()));

    assertThat(line).doesNotContain("\n");
    assertThat(line).startsWith("✅ Está bem · ✅ Tomou os medicamentos · ⚠️ Relatou");
  }

  private static ResumoLigacao summary(NivelAlerta level, List<String> reasons) {
    return new ResumoLigacao(
        "Tudo certo.",
        Humor.GOOD,
        List.of(
            item("wellbeing", StatusResposta.OK, "Está bem"),
            item("medication", StatusResposta.OK, "Tomou os medicamentos"),
            item("pain", StatusResposta.ATTENTION, "Relatou dor no joelho"),
            new RespostaAtributo("sleep", "Dormiu bem?", "", ClarezaResposta.NOT_ASKED, null, null),
            item("message_for_family", StatusResposta.OK, "Deixou um recado")),
        List.of(),
        level,
        reasons,
        false,
        "Vou ao médico na segunda-feira.");
  }

  private static RespostaAtributo item(String key, StatusResposta status, String headline) {
    return new RespostaAtributo(key, "?", "resposta", ClarezaResposta.CLEAR, status, headline);
  }
}
