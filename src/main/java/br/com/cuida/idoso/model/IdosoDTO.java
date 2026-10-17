package br.com.cuida.idoso.model;

import java.time.ZoneId;
import java.util.UUID;

/**
 * Dados do idoso que os outros módulos podem usar (API pública do módulo).
 *
 * @param preferredName como a pessoa gosta de ser chamada (pode ser {@code null})
 * @param phoneNumber telefone em E.164
 * @param zone fuso da pessoa idosa: base do calendário e da agenda
 * @param guardianName responsável que recebe o resumo (pode ser {@code null})
 * @param guardianWhatsapp WhatsApp do responsável em E.164 (pode ser {@code null})
 */
public record IdosoDTO(
    UUID id,
    UUID tutorId,
    String name,
    String preferredName,
    String phoneNumber,
    ZoneId zone,
    boolean recordingConsent,
    boolean active,
    String guardianName,
    String guardianWhatsapp) {

  /** Nome usado na conversa: o apelido, se houver. */
  public String nameForConversation() {
    return preferredName != null ? preferredName : name;
  }
}
