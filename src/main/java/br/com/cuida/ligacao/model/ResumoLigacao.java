package br.com.cuida.ligacao.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Resumo estruturado de uma ligação, persistido em {@code calls.structured_summary} (jsonb).
 *
 * <p>O tipo pertence ao módulo {@code ligacao} porque é ele quem guarda o dado; o módulo {@code
 * analise} apenas o produz.
 *
 * @param overview 2–4 frases em linguagem simples
 * @param mood humor percebido
 * @param answers uma resposta por item do questionário vigente
 * @param notableMentions fatos relevantes que surgiram fora das perguntas
 * @param alertLevel nível de alerta
 * @param alertReasons motivos do alerta (vazio quando {@link NivelAlerta#NONE})
 * @param followUpSuggested se vale o tutor entrar em contato
 * @param familyMessage recado da pessoa para a família ({@code null} se não houver)
 */
public record ResumoLigacao(
    @NotBlank @Size(max = 800) String overview,
    @NotNull Humor mood,
    @NotNull List<@Valid @NotNull RespostaAtributo> answers,
    @NotNull List<@NotBlank @Size(max = 300) String> notableMentions,
    @NotNull NivelAlerta alertLevel,
    @NotNull List<@NotBlank @Size(max = 300) String> alertReasons,
    boolean followUpSuggested,
    @Size(max = 500) String familyMessage) {}
