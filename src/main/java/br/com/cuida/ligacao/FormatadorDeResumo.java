package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.ItemChecklistDTO;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoFormatadoDTO;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusResposta;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Monta o resumo no formato do pitch, usado na tela e na mensagem para a família:
 *
 * <pre>
 * ALÔ DIÁRIO – Dona Maria
 * Data: 17/10
 * ✅ Está bem
 * ⚠️ Relatou dor no joelho
 * Recado: "Vou ao médico na segunda-feira."
 * </pre>
 *
 * A pergunta do recado ({@value #FAMILY_MESSAGE_KEY}) não entra no checklist: vira a linha
 * "Recado".
 */
@Component
public class FormatadorDeResumo {

  /** Chave reservada da pergunta do recado para a família. */
  public static final String FAMILY_MESSAGE_KEY = "message_for_family";

  private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

  private final LigacaoProperties ligacaoProperties;

  FormatadorDeResumo(LigacaoProperties ligacaoProperties) {
    this.ligacaoProperties = ligacaoProperties;
  }

  public ResumoFormatadoDTO formatar(ResumoLigacao resumo, String nome, LocalDate data) {
    List<ItemChecklistDTO> checklist = checklist(resumo);
    String recado = blankToNull(resumo.familyMessage());
    StringBuilder texto = new StringBuilder();
    texto
        .append(ligacaoProperties.productName().toUpperCase(Locale.forLanguageTag("pt-BR")))
        .append(" – ")
        .append(nome)
        .append('\n')
        .append("Data: ")
        .append(data.format(DAY_MONTH))
        .append('\n');
    if (resumo.alertLevel() == NivelAlerta.URGENT) {
      texto.append("🚨 Atenção: ").append(String.join("; ", resumo.alertReasons())).append('\n');
    }
    for (ItemChecklistDTO item : checklist) {
      texto.append(icone(item.status())).append(' ').append(item.text()).append('\n');
    }
    if (resumo.alertLevel() == NivelAlerta.ATTENTION && !resumo.alertReasons().isEmpty()) {
      texto.append("⚠️ ").append(String.join("; ", resumo.alertReasons())).append('\n');
    }
    if (recado != null) {
      texto.append("Recado: \"").append(recado).append('"').append('\n');
    }
    return new ResumoFormatadoDTO(checklist, recado, texto.toString().trim());
  }

  /** Itens do checklist em uma linha só, para parâmetros de template (a Meta não aceita quebra). */
  public String checklistEmUmaLinha(ResumoLigacao resumo) {
    return String.join(
        " · ", checklist(resumo).stream().map(i -> icone(i.status()) + " " + i.text()).toList());
  }

  public static String icone(StatusResposta status) {
    return switch (status) {
      case OK -> "✅";
      case ATTENTION -> "⚠️";
      case UNKNOWN -> "❔";
    };
  }

  private static List<ItemChecklistDTO> checklist(ResumoLigacao resumo) {
    return resumo.answers().stream()
        .filter(a -> !FAMILY_MESSAGE_KEY.equals(a.attributeKey()))
        .map(a -> new ItemChecklistDTO(a.attributeKey(), a.statusOrUnknown(), texto(a)))
        .toList();
  }

  private static String texto(RespostaAtributo resposta) {
    if (resposta.headline() != null && !resposta.headline().isBlank()) {
      return resposta.headline().trim();
    }
    if (resposta.answer() != null && !resposta.answer().isBlank()) {
      return resposta.answer().trim();
    }
    return resposta.question() + " (sem resposta)";
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
