package br.com.cuida.call.vapi;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Integração com o Vapi ({@code app.voice.provider=vapi}). Modelo, voz e transcritor ficam
 * configuráveis porque a escolha ideal para idosos em pt-BR precisa ser testada com pessoas reais.
 *
 * @param apiKey chave privada da API ({@code VAPI_API_KEY})
 * @param phoneNumberId número de saída cadastrado no Vapi ({@code VAPI_PHONE_NUMBER_ID})
 * @param webhookBaseUrl URL pública deste backend; o webhook vai para {@code /webhook/vapi}
 * @param webhookSecret segredo enviado no header do webhook (mesmo {@code VAPI_WEBHOOK_SECRET})
 * @param webhookSecretHeader nome do header (o mesmo validado pelo módulo webhook)
 */
@ConfigurationProperties("app.voice.vapi")
public record VapiProperties(
    String apiKey,
    String phoneNumberId,
    @DefaultValue("https://api.vapi.ai") String baseUrl,
    String webhookBaseUrl,
    String webhookSecret,
    @DefaultValue("X-Webhook-Secret") String webhookSecretHeader,
    @DefaultValue("PT15S") Duration timeout,
    @DefaultValue Model model,
    @DefaultValue Voice voice,
    @DefaultValue Transcriber transcriber) {

  public record Model(
      @DefaultValue("openai") String provider,
      @DefaultValue("gpt-4o") String model,
      @DefaultValue("0.4") double temperature) {}

  public record Voice(
      @DefaultValue("azure") String provider,
      @DefaultValue("pt-BR-FranciscaNeural") String voiceId) {}

  public record Transcriber(
      @DefaultValue("deepgram") String provider,
      @DefaultValue("nova-2") String model,
      @DefaultValue("pt-BR") String language) {}
}
