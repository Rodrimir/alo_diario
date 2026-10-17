package br.com.cuida.questionnaire;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Um item do corpo de {@code PUT /api/elderly/{id}/questionnaire} (lista). A ordem da lista vira a
 * ordem das perguntas na conversa.
 *
 * @param attributeKey chave estável do atributo, ex.: {@code sleep}, {@code medication}
 * @param active padrão {@code true}; inativo fica guardado, mas não é perguntado
 */
record QuestionnaireItemRequest(
    @NotBlank @Pattern(
            regexp = "^[a-z][a-z0-9_]{1,39}$",
            message = "use de 2 a 40 letras minúsculas, números ou _, começando por letra")
        String attributeKey,
    @NotBlank @Size(max = 200) String questionText,
    @NotNull AnswerType answerType,
    Boolean active) {}
