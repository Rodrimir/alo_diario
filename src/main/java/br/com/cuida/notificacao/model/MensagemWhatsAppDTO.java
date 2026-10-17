package br.com.cuida.notificacao.model;

import java.util.Map;

/**
 * Mensagem de template para o WhatsApp.
 *
 * @param destino WhatsApp em E.164
 * @param template nome do template aprovado na Meta
 * @param variaveis parâmetros nomeados do template (sem quebra de linha: regra da Meta)
 */
public record MensagemWhatsAppDTO(String destino, String template, Map<String, String> variaveis) {

  public MensagemWhatsAppDTO {
    variaveis = Map.copyOf(variaveis);
  }
}
