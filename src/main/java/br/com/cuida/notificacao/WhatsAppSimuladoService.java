package br.com.cuida.notificacao;

import br.com.cuida.notificacao.model.MensagemWhatsAppDTO;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Envio simulado (padrão no dev, sem chave): não manda nada, só registra no log o template e os
 * parâmetros. O texto completo fica salvo na notificação e aparece na API.
 */
@Service
@Profile("!test")
@ConditionalOnProperty(
    name = "app.notifications.provider",
    havingValue = "mock",
    matchIfMissing = true)
class WhatsAppSimuladoService implements WhatsAppService {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppSimuladoService.class);

  @Override
  public String enviarTemplate(MensagemWhatsAppDTO mensagem) {
    log.info(
        "[WhatsApp simulado] template {} para {}: {}",
        mensagem.template(),
        mensagem.destino(),
        mensagem.variaveis());
    return "simulado-" + UUID.randomUUID();
  }
}
