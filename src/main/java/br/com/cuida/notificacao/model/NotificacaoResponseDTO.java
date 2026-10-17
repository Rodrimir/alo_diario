package br.com.cuida.notificacao.model;

import br.com.cuida.infra.util.HorarioLocal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Aviso enviado (ou não) à família. Datas no fuso do idoso.
 *
 * @param recipient WhatsApp do responsável em E.164 ({@code null} se não cadastrado)
 * @param content texto completo do aviso (o resumo no formato do pitch)
 * @param error motivo da falha ou de não ter sido enviado
 */
public record NotificacaoResponseDTO(
    UUID id,
    UUID callId,
    TipoNotificacao kind,
    String channel,
    String recipient,
    String template,
    StatusNotificacao status,
    String content,
    int attempts,
    String error,
    LocalDateTime createdAt,
    LocalDateTime sentAt) {

  public static NotificacaoResponseDTO of(Notificacao n, ZoneId zone) {
    return new NotificacaoResponseDTO(
        n.getId(),
        n.getCallId(),
        n.getKind(),
        n.getChannel(),
        n.getRecipient(),
        n.getTemplate(),
        n.getStatus(),
        n.getContent(),
        n.getAttempts(),
        n.getError(),
        HorarioLocal.at(n.getCreatedAt(), zone),
        HorarioLocal.at(n.getSentAt(), zone));
  }
}
