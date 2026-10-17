package br.com.cuida.notificacao.model;

import br.com.cuida.infra.persistencia.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Um aviso à família. A {@code dedupKey} única garante que o mesmo aviso não seja enviado duas
 * vezes, mesmo com o evento reprocessado pelo outbox.
 */
@Entity
@Table(name = "notifications")
public class Notificacao extends EntidadeAuditavel {

  public static final String CHANNEL_WHATSAPP = "WHATSAPP";
  public static final int ERROR_MAX_LENGTH = 500;

  @Column(name = "elderly_id", nullable = false, updatable = false)
  private UUID elderlyId;

  @Column(name = "call_id", updatable = false)
  private UUID callId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private TipoNotificacao kind;

  @Column(nullable = false, updatable = false, length = 20)
  private String channel = CHANNEL_WHATSAPP;

  @Column(length = 16, updatable = false)
  private String recipient;

  @Column(nullable = false, updatable = false, length = 100)
  private String template;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusNotificacao status = StatusNotificacao.PENDING;

  @Column(nullable = false, updatable = false, columnDefinition = "text")
  private String content;

  /** Parâmetros do template, guardados para o reenvio. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, updatable = false)
  private Map<String, String> variables;

  @Column(name = "provider_message_id", length = 200)
  private String providerMessageId;

  @Column(nullable = false)
  private int attempts;

  @Column(length = ERROR_MAX_LENGTH)
  private String error;

  @Column(name = "dedup_key", nullable = false, updatable = false, length = 200)
  private String dedupKey;

  @Column(name = "sent_at")
  private Instant sentAt;

  /** Exigido pelo JPA. */
  protected Notificacao() {}

  public Notificacao(
      UUID elderlyId,
      UUID callId,
      TipoNotificacao kind,
      String recipient,
      String template,
      String content,
      Map<String, String> variables,
      String dedupKey) {
    super(UUID.randomUUID());
    this.elderlyId = Objects.requireNonNull(elderlyId, "elderlyId");
    this.callId = callId;
    this.kind = Objects.requireNonNull(kind, "kind");
    this.recipient = recipient;
    this.template = Objects.requireNonNull(template, "template");
    this.content = Objects.requireNonNull(content, "content");
    // LinkedHashMap: a ordem dos parâmetros é a que vai para a Meta, inclusive no reenvio.
    this.variables = new LinkedHashMap<>(variables);
    this.dedupKey = Objects.requireNonNull(dedupKey, "dedupKey");
  }

  public void markSent(String providerMessageId, Instant at) {
    this.status = StatusNotificacao.SENT;
    this.providerMessageId = providerMessageId;
    this.sentAt = at;
    this.error = null;
    this.attempts++;
  }

  public void markFailed(String error) {
    this.status = StatusNotificacao.FAILED;
    this.error =
        error == null || error.length() <= ERROR_MAX_LENGTH
            ? error
            : error.substring(0, ERROR_MAX_LENGTH);
    this.attempts++;
  }

  public void markSkipped(String reason) {
    this.status = StatusNotificacao.SKIPPED;
    this.error = reason;
  }

  public UUID getElderlyId() {
    return elderlyId;
  }

  public UUID getCallId() {
    return callId;
  }

  public TipoNotificacao getKind() {
    return kind;
  }

  public String getChannel() {
    return channel;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getTemplate() {
    return template;
  }

  public StatusNotificacao getStatus() {
    return status;
  }

  public String getContent() {
    return content;
  }

  public Map<String, String> getVariables() {
    return variables;
  }

  /** A mensagem a enviar (ou reenviar) ao WhatsApp do responsável. */
  public MensagemWhatsAppDTO toMensagem() {
    return new MensagemWhatsAppDTO(recipient, template, variables);
  }

  public String getProviderMessageId() {
    return providerMessageId;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getError() {
    return error;
  }

  public String getDedupKey() {
    return dedupKey;
  }

  public Instant getSentAt() {
    return sentAt;
  }
}
