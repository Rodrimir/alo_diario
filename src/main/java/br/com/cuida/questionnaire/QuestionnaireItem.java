package br.com.cuida.questionnaire;

import br.com.cuida.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Pergunta/atributo que a IA deve cobrir na conversa com a pessoa idosa. */
@Entity
@Table(name = "questionnaire_items")
class QuestionnaireItem extends AuditableEntity {

  @Column(name = "elderly_id", nullable = false, updatable = false)
  private UUID elderlyId;

  /** Chave estável do atributo (ex.: {@code sleep}, {@code medication}). */
  @Column(name = "attribute_key", nullable = false, length = 40)
  private String attributeKey;

  @Column(name = "question_text", nullable = false, length = 200)
  private String questionText;

  @Enumerated(EnumType.STRING)
  @Column(name = "answer_type", nullable = false, length = 20)
  private AnswerType answerType;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false)
  private boolean active = true;

  /** Exigido pelo JPA. */
  protected QuestionnaireItem() {}

  QuestionnaireItem(
      UUID elderlyId,
      String attributeKey,
      String questionText,
      AnswerType answerType,
      int position,
      boolean active) {
    super(UUID.randomUUID());
    this.elderlyId = Objects.requireNonNull(elderlyId, "elderlyId");
    this.attributeKey = Objects.requireNonNull(attributeKey, "attributeKey");
    this.questionText = Objects.requireNonNull(questionText, "questionText");
    this.answerType = Objects.requireNonNull(answerType, "answerType");
    this.position = position;
    this.active = active;
  }

  void update(String questionText, AnswerType answerType, int position, boolean active) {
    this.questionText = Objects.requireNonNull(questionText, "questionText").trim();
    this.answerType = Objects.requireNonNull(answerType, "answerType");
    this.position = position;
    this.active = active;
  }

  UUID getElderlyId() {
    return elderlyId;
  }

  String getAttributeKey() {
    return attributeKey;
  }

  String getQuestionText() {
    return questionText;
  }

  AnswerType getAnswerType() {
    return answerType;
  }

  int getPosition() {
    return position;
  }

  boolean isActive() {
    return active;
  }

  QuestionnaireSnapshot.Item toSnapshotItem() {
    return new QuestionnaireSnapshot.Item(attributeKey, questionText, answerType, position);
  }
}
