package br.com.cuida.call;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Resumo estruturado de uma ligação, persistido em {@code calls.structured_summary} (jsonb).
 *
 * <p>O tipo pertence ao módulo {@code call} porque é ele quem guarda o dado; o módulo {@code
 * analysis} apenas o produz.
 *
 * @param overview 2–4 frases em linguagem simples
 * @param mood humor percebido
 * @param answers uma resposta por item do questionário vigente
 * @param notableMentions fatos relevantes que surgiram fora das perguntas
 * @param alertLevel nível de alerta
 * @param alertReasons motivos do alerta (vazio quando {@link AlertLevel#NONE})
 * @param followUpSuggested se vale o tutor entrar em contato
 */
public record CallSummary(
    @NotBlank @Size(max = 800) String overview,
    @NotNull Mood mood,
    @NotNull List<@Valid @NotNull AttributeAnswer> answers,
    @NotNull List<@NotBlank @Size(max = 300) String> notableMentions,
    @NotNull AlertLevel alertLevel,
    @NotNull List<@NotBlank @Size(max = 300) String> alertReasons,
    boolean followUpSuggested) {}
