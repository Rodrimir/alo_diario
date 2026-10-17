package br.com.cuida.questionario.model;

import java.util.UUID;

/** Pergunta do questionário de um idoso. */
public record ItemQuestionarioResponseDTO(
    UUID id,
    String attributeKey,
    String questionText,
    TipoResposta answerType,
    int position,
    boolean active) {

  public static ItemQuestionarioResponseDTO of(ItemQuestionario item) {
    return new ItemQuestionarioResponseDTO(
        item.getId(),
        item.getAttributeKey(),
        item.getQuestionText(),
        item.getAnswerType(),
        item.getPosition(),
        item.isActive());
  }
}
