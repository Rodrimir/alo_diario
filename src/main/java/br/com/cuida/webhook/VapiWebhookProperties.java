package br.com.cuida.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Validação do webhook do Vapi.
 *
 * @param secret valor esperado no header ({@code VAPI_WEBHOOK_SECRET}); vazio = todo webhook é
 *     recusado
 * @param secretHeader nome do header (o mesmo enviado em {@code assistant.server.headers})
 */
@ConfigurationProperties("app.webhook.vapi")
record VapiWebhookProperties(String secret, @DefaultValue("X-Webhook-Secret") String secretHeader) {

  boolean configured() {
    return secret != null && !secret.isBlank();
  }
}
