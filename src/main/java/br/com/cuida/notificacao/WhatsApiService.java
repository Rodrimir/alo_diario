package br.com.cuida.notificacao;

import br.com.cuida.notificacao.model.MensagemWhatsAppDTO;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * Envio real pelo whats-api da equipe: {@code POST /api/messages/template} com a {@code api_key} da
 * empresa (que já resolve o número de origem e conta como admin, exigido para template).
 *
 * <p>Sem retry aqui (o {@code ReenvioNotificacaoJob} tenta de novo); circuit breaker para não
 * insistir com o whats-api fora do ar.
 */
@Service
@Profile("!test")
@ConditionalOnProperty(name = "app.notifications.provider", havingValue = "whatsapi")
class WhatsApiService implements WhatsAppService {

  private static final Duration TEMPO_LIMITE_CONEXAO = Duration.ofSeconds(10);
  private static final Duration TEMPO_LIMITE_RESPOSTA = Duration.ofSeconds(20);

  private final RestClient restClient;
  private final NotificacaoProperties properties;
  private final CircuitBreaker circuitBreaker;

  @Autowired
  WhatsApiService(NotificacaoProperties properties) {
    this(RestClient.builder().requestFactory(fabricaDeRequisicoes()), properties);
  }

  /** Para testes: usa o builder como veio (ex.: ligado a um servidor simulado). */
  WhatsApiService(RestClient.Builder builder, NotificacaoProperties properties) {
    NotificacaoProperties.WhatsApi whatsapi = properties.whatsapi();
    if (blank(whatsapi.baseUrl()) || blank(whatsapi.apiKey())) {
      throw new IllegalStateException(
          "app.notifications.provider=whatsapi exige WHATSAPI_BASE_URL e WHATSAPI_API_KEY");
    }
    this.restClient =
        builder
            .baseUrl(whatsapi.baseUrl())
            .defaultHeader("Authorization", "Bearer " + whatsapi.apiKey())
            .build();
    this.properties = properties;
    this.circuitBreaker =
        CircuitBreaker.of(
            "whatsapi",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(2))
                .build());
  }

  @Override
  public String enviarTemplate(MensagemWhatsAppDTO mensagem) {
    Map<String, Object> corpo =
        Map.of(
            "phoneNumber", mensagem.destino(),
            "templateName", mensagem.template(),
            "languageCode", properties.whatsapi().languageCode(),
            "variables",
                mensagem.variaveis().entrySet().stream()
                    .map(e -> Map.of("key", e.getKey(), "value", e.getValue()))
                    .toList());
    try {
      JsonNode resposta = circuitBreaker.executeSupplier(() -> post(corpo));
      String id =
          resposta == null
              ? ""
              : resposta.path("whatsappMessageId").asString(resposta.path("id").asString(""));
      return id.isBlank() ? "sem-id" : id;
    } catch (CallNotPermittedException e) {
      throw new EnvioWhatsAppException("whats-api indisponível (circuit breaker aberto)", e);
    } catch (RestClientException e) {
      throw new EnvioWhatsAppException("whats-api recusou o envio: " + e.getMessage(), e);
    }
  }

  private JsonNode post(Map<String, Object> corpo) {
    return restClient
        .post()
        .uri("/api/messages/template")
        .contentType(MediaType.APPLICATION_JSON)
        .body(corpo)
        .retrieve()
        .body(JsonNode.class);
  }

  private static JdkClientHttpRequestFactory fabricaDeRequisicoes() {
    JdkClientHttpRequestFactory fabrica =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE_CONEXAO).build());
    fabrica.setReadTimeout(TEMPO_LIMITE_RESPOSTA);
    return fabrica;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
