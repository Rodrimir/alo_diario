package br.com.cuida.ligacao.vapi;

import br.com.cuida.ligacao.GatewayDeVoz;
import br.com.cuida.ligacao.LigacaoRecusadaException;
import br.com.cuida.ligacao.model.LigacaoIniciada;
import br.com.cuida.ligacao.model.PedidoDeLigacao;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * Liga pelo Vapi ({@code POST /call}) com um assistente <b>transitório</b>, montado a cada ligação
 * com as perguntas do idoso. Como o Vapi não envia as credenciais da organização para assistentes
 * transitórios, o segredo do webhook vai em {@code assistant.server.headers}.
 *
 * <p>Sem retry (um POST que estourou o tempo pode já ter criado a ligação); só circuit breaker,
 * para não insistir quando o Vapi está fora do ar.
 */
@Component
@ConditionalOnProperty(name = "app.voice.provider", havingValue = "vapi")
class GatewayDeVozVapi implements GatewayDeVoz {

  static final String PROVIDER = "vapi";

  private final RestClient http;
  private final VapiProperties vapiProperties;
  private final CircuitBreaker circuitBreaker;

  @Autowired
  GatewayDeVozVapi(VapiProperties vapiProperties) {
    this(RestClient.builder().requestFactory(requestFactory(vapiProperties)), vapiProperties);
  }

  /** Para testes: usa o builder como veio (ex.: ligado a um servidor simulado). */
  GatewayDeVozVapi(RestClient.Builder builder, VapiProperties vapiProperties) {
    require(vapiProperties.apiKey(), "VAPI_API_KEY");
    require(vapiProperties.phoneNumberId(), "VAPI_PHONE_NUMBER_ID");
    require(vapiProperties.webhookBaseUrl(), "VAPI_WEBHOOK_BASE_URL");
    require(vapiProperties.webhookSecret(), "VAPI_WEBHOOK_SECRET");
    this.http =
        builder
            .baseUrl(vapiProperties.baseUrl())
            .defaultHeader("Authorization", "Bearer " + vapiProperties.apiKey())
            .build();
    this.vapiProperties = vapiProperties;
    this.circuitBreaker =
        CircuitBreaker.of(
            "vapi",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(2))
                .build());
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public LigacaoIniciada start(PedidoDeLigacao call) {
    try {
      JsonNode response = circuitBreaker.executeSupplier(() -> post(requestBody(call)));
      String id = response == null ? "" : response.path("id").asString("");
      if (id.isBlank()) {
        throw new LigacaoRecusadaException("resposta do Vapi sem id da ligação");
      }
      return new LigacaoIniciada(id);
    } catch (CallNotPermittedException e) {
      throw new LigacaoRecusadaException("Vapi indisponível (circuit breaker aberto)", e);
    } catch (RestClientException e) {
      throw new LigacaoRecusadaException("Vapi recusou a ligação: " + e.getMessage(), e);
    }
  }

  private JsonNode post(Map<String, Object> body) {
    return http.post()
        .uri("/call")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);
  }

  Map<String, Object> requestBody(PedidoDeLigacao call) {
    Map<String, Object> metadata = Map.of("callId", call.callId().toString());

    Map<String, Object> assistant = new LinkedHashMap<>();
    assistant.put("name", "Alô Diário");
    assistant.put("firstMessage", call.firstMessage());
    assistant.put("firstMessageMode", "assistant-speaks-first");
    assistant.put(
        "model",
        Map.of(
            "provider", vapiProperties.model().provider(),
            "model", vapiProperties.model().model(),
            "temperature", vapiProperties.model().temperature(),
            "messages", List.of(Map.of("role", "system", "content", call.systemPrompt()))));
    assistant.put(
        "voice",
        Map.of(
            "provider",
            vapiProperties.voice().provider(),
            "voiceId",
            vapiProperties.voice().voiceId()));
    assistant.put(
        "transcriber",
        Map.of(
            "provider", vapiProperties.transcriber().provider(),
            "model", vapiProperties.transcriber().model(),
            "language", vapiProperties.transcriber().language()));
    assistant.put("maxDurationSeconds", call.maxDurationSeconds());
    assistant.put("endCallFunctionEnabled", true);
    assistant.put("serverMessages", List.of("status-update", "end-of-call-report"));
    assistant.put(
        "server",
        Map.of(
            "url",
            stripTrailingSlash(vapiProperties.webhookBaseUrl()) + "/webhook/vapi",
            "headers",
            Map.of(vapiProperties.webhookSecretHeader(), vapiProperties.webhookSecret())));
    assistant.put("artifactPlan", Map.of("recordingEnabled", true));
    assistant.put("metadata", metadata);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("phoneNumberId", vapiProperties.phoneNumberId());
    body.put("customer", Map.of("number", call.phoneNumber(), "name", call.elderlyName()));
    body.put("assistant", assistant);
    body.put("metadata", metadata);
    return body;
  }

  private static JdkClientHttpRequestFactory requestFactory(VapiProperties vapiProperties) {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(vapiProperties.timeout());
    return factory;
  }

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static void require(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("app.voice.provider=vapi exige " + name);
    }
  }
}
