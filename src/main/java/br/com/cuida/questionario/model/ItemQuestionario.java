package br.com.cuida.questionario.model;

import br.com.cuida.infra.persistencia.EntidadeAuditavel;
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
public class ItemQuestionario extends EntidadeAuditavel {

  @Column(name = "elderly_id", nullable = false, updatable = false)
  private UUID elderlyId;

  /** Chave estável do atributo (ex.: {@code sleep}, {@code medication}). */
  @Column(name = "attribute_key", nullable = false, length = 40)
  private String attributeKey;

  @Column(name = "question_text", nullable = false, length = 200)
  private String questionText;

  @Enumerated(EnumType.STRING)
  @Column(name = "answer_type", nullable = false, length = 20)
  private TipoResposta answerType;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false)
  private boolean active = true;

  /** Exigido pelo JPA. */
  protected ItemQuestionario() {}

  public ItemQuestionario(
      UUID elderlyId,
      String attributeKey,
      String questionText,
      TipoResposta answerType,
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

  public void update(String questionText, TipoResposta answerType, int position, boolean active) {
    this.questionText = Objects.requireNonNull(questionText, "questionText").trim();
    this.answerType = Objects.requireNonNull(answerType, "answerType");
    this.position = position;
    this.active = active;
  }

  public UUID getElderlyId() {
    return elderlyId;
  }

  public String getAttributeKey() {
    return attributeKey;
  }

  public String getQuestionText() {
    return questionText;
  }

  public TipoResposta getAnswerType() {
    return answerType;
  }

  public int getPosition() {
    return position;
  }

  public boolean isActive() {
    return active;
  }

  public QuestionarioSnapshot.Item toSnapshotItem() {
    return new QuestionarioSnapshot.Item(attributeKey, questionText, answerType, position);
  }
}
