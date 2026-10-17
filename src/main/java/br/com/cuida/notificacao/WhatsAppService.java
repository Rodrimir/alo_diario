package br.com.cuida.notificacao;

import br.com.cuida.notificacao.model.MensagemWhatsAppDTO;

/**
 * Envio de mensagens de WhatsApp (templates aprovados pela Meta). Implementações: {@link
 * WhatsApiService} (real, pelo whats-api), {@link WhatsAppSimuladoService} (dev) e um fake nos
 * testes.
 */
public interface WhatsAppService {

  /**
   * @return id da mensagem no provedor
   * @throws EnvioWhatsAppException se o envio for recusado ou não puder ser feito
   */
  String enviarTemplate(MensagemWhatsAppDTO mensagem);
}
