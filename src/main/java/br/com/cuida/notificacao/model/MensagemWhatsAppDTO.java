package br.com.cuida.notificacao.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mensagem de template para o WhatsApp.
 *
 * @param destino WhatsApp em E.164
 * @param template nome do template aprovado na Meta
 * @param variaveis parâmetros nomeados do template (sem quebra de linha: regra da Meta). A ordem de
 *     inserção é preservada, porque é a ordem em que os parâmetros vão para a Meta.
 */
public record MensagemWhatsAppDTO(String destino, String template, Map<String, String> variaveis) {

  public MensagemWhatsAppDTO {
    variaveis = Collections.unmodifiableMap(new LinkedHashMap<>(variaveis));
  }
}
