package br.com.cuida.ligacao.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resposta a um item do questionário.
 *
 * @param attributeKey chave do item (igual à do snapshot)
 * @param question pergunta feita
 * @param answer o que a pessoa disse, resumido (vazio se não perguntado/respondido)
 * @param clarity clareza da resposta
 */
public record RespostaAtributo(
    @NotBlank @Size(max = 40) String attributeKey,
    @NotBlank @Size(max = 200) String question,
    @NotNull @Size(max = 500) String answer,
    @NotNull ClarezaResposta clarity) {}
