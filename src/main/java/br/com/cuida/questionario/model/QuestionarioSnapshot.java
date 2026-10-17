package br.com.cuida.questionario.model;

import java.util.List;

/**
 * Cópia imutável do questionário vigente no momento em que uma ligação foi criada. Fica gravada na
 * própria ligação, preservando o histórico mesmo que o tutor altere as perguntas depois.
 */
public record QuestionarioSnapshot(List<Item> items) {

  public QuestionarioSnapshot {
    items = List.copyOf(items);
  }

  /** Uma pergunta congelada no snapshot. */
  public record Item(
      String attributeKey, String questionText, TipoResposta answerType, int position) {}
}
