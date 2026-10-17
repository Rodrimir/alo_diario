package br.com.cuida.ligacao.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resposta a um item do questionário.
 *
 * <p>Resumos gravados antes de {@code status}/{@code headline} existirem continuam válidos: os
 * campos ausentes são lidos como {@code null} e tratados como {@link StatusResposta#UNKNOWN}.
 *
 * @param attributeKey chave do item (igual à do snapshot)
 * @param question pergunta feita
 * @param answer o que a pessoa disse, resumido (vazio se não perguntado/respondido)
 * @param clarity clareza da resposta
 * @param status ✅/⚠️/❔ do checklist
 * @param headline frase curta do checklist (ex.: "Tomou os medicamentos")
 */
public record RespostaAtributo(
    @NotBlank @Size(max = 40) String attributeKey,
    @NotBlank @Size(max = 200) String question,
    @NotNull @Size(max = 500) String answer,
    @NotNull ClarezaResposta clarity,
    StatusResposta status,
    @Size(max = 80) String headline) {

  /** Status efetivo: {@code UNKNOWN} quando ausente (resumos antigos). */
  public StatusResposta statusOrUnknown() {
    return status != null ? status : StatusResposta.UNKNOWN;
  }
}
