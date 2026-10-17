package br.com.cuida.notificacao;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Avisos à família pelo WhatsApp.
 *
 * @param provider {@code mock} (padrão: só registra no log) ou {@code whatsapi}
 * @param whatsapi conexão com a API de WhatsApp da equipe
 * @param templates nomes dos templates aprovados na Meta
 */
@ConfigurationProperties("app.notifications")
public record NotificacaoProperties(
    @DefaultValue("mock") String provider,
    @DefaultValue WhatsApi whatsapi,
    @DefaultValue Templates templates) {

  /**
   * @param baseUrl endereço do whats-api ({@code WHATSAPI_BASE_URL})
   * @param apiKey {@code api_key} da empresa no whats-api ({@code WHATSAPI_API_KEY})
   * @param languageCode idioma dos templates
   */
  public record WhatsApi(
      String baseUrl, String apiKey, @DefaultValue("pt_BR") String languageCode) {}

  public record Templates(
      @DefaultValue("alo_diario_resumo") String resumo,
      @DefaultValue("alo_diario_sem_contato") String semContato,
      @DefaultValue("alo_diario_alerta") String alerta) {}
}
