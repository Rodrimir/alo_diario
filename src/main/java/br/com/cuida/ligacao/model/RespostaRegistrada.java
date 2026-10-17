package br.com.cuida.ligacao.model;

import br.com.cuida.infra.persistencia.EntidadeBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Resposta registrada pela IA de voz durante a ligação (ferramenta MCP {@code registrar_resposta}
 * ou {@code registrar_recado}). Tem prioridade sobre o que o resumidor extrair da transcrição.
 */
@Entity
@Table(name = "call_answers")
public class RespostaRegistrada extends EntidadeBase {

  @Column(name = "call_id", nullable = false, updatable = false)
  private UUID callId;

  @Column(name = "attribute_key", nullable = false, updatable = false, length = 60)
  private String attributeKey;

  @Column(length = 300)
  private String question;

  @Column(nullable = false, length = 500)
  private String answer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusResposta status;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  /** Exigido pelo JPA. */
  protected RespostaRegistrada() {}

  public RespostaRegistrada(
      UUID callId,
      String attributeKey,
      String question,
      String answer,
      StatusResposta status,
      Instant at) {
    super(UUID.randomUUID());
    this.callId = Objects.requireNonNull(callId, "callId");
    this.attributeKey = Objects.requireNonNull(attributeKey, "attributeKey");
    update(question, answer, status, at);
  }

  /** A IA pode corrigir a resposta na mesma ligação: vale a última. */
  public void update(String question, String answer, StatusResposta status, Instant at) {
    this.question = question;
    this.answer = Objects.requireNonNull(answer, "answer");
    this.status = Objects.requireNonNull(status, "status");
    this.recordedAt = Objects.requireNonNull(at, "at");
  }

  public RespostaRegistradaDTO toDTO() {
    return new RespostaRegistradaDTO(attributeKey, question, answer, status);
  }

  public UUID getCallId() {
    return callId;
  }

  public String getAttributeKey() {
    return attributeKey;
  }
}
