package br.com.cuida.call.vapi;

import br.com.cuida.call.OutboundCall;
import br.com.cuida.call.StartedCall;
import br.com.cuida.call.VoiceCallException;
import br.com.cuida.call.VoiceCallGateway;
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
class VapiVoiceCallGateway implements VoiceCallGateway {

  static final String PROVIDER = "vapi";

  private final RestClient http;
  private final VapiProperties properties;
  private final CircuitBreaker circuitBreaker;

  @Autowired
  VapiVoiceCallGateway(VapiProperties properties) {
    this(RestClient.builder().requestFactory(requestFactory(properties)), properties);
  }

  /** Para testes: usa o builder como veio (ex.: ligado a um servidor simulado). */
  VapiVoiceCallGateway(RestClient.Builder builder, VapiProperties properties) {
    require(properties.apiKey(), "VAPI_API_KEY");
    require(properties.phoneNumberId(), "VAPI_PHONE_NUMBER_ID");
    require(properties.webhookBaseUrl(), "VAPI_WEBHOOK_BASE_URL");
    require(properties.webhookSecret(), "VAPI_WEBHOOK_SECRET");
    this.http =
        builder
            .baseUrl(properties.baseUrl())
            .defaultHeader("Authorization", "Bearer " + properties.apiKey())
            .build();
    this.properties = properties;
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
  public StartedCall start(OutboundCall call) {
    try {
      JsonNode response = circuitBreaker.executeSupplier(() -> post(requestBody(call)));
      String id = response == null ? "" : response.path("id").asString("");
      if (id.isBlank()) {
        throw new VoiceCallException("resposta do Vapi sem id da ligação");
      }
      return new StartedCall(id);
    } catch (CallNotPermittedException e) {
      throw new VoiceCallException("Vapi indisponível (circuit breaker aberto)", e);
    } catch (RestClientException e) {
      throw new VoiceCallException("Vapi recusou a ligação: " + e.getMessage(), e);
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

  Map<String, Object> requestBody(OutboundCall call) {
    Map<String, Object> metadata = Map.of("callId", call.callId().toString());

    Map<String, Object> assistant = new LinkedHashMap<>();
    assistant.put("name", "Cuida");
    assistant.put("firstMessage", call.firstMessage());
    assistant.put("firstMessageMode", "assistant-speaks-first");
    assistant.put(
        "model",
        Map.of(
            "provider", properties.model().provider(),
            "model", properties.model().model(),
            "temperature", properties.model().temperature(),
            "messages", List.of(Map.of("role", "system", "content", call.systemPrompt()))));
    assistant.put(
        "voice",
        Map.of("provider", properties.voice().provider(), "voiceId", properties.voice().voiceId()));
    assistant.put(
        "transcriber",
        Map.of(
            "provider", properties.transcriber().provider(),
            "model", properties.transcriber().model(),
            "language", properties.transcriber().language()));
    assistant.put("maxDurationSeconds", call.maxDurationSeconds());
    assistant.put("endCallFunctionEnabled", true);
    assistant.put("serverMessages", List.of("status-update", "end-of-call-report"));
    assistant.put(
        "server",
        Map.of(
            "url",
            stripTrailingSlash(properties.webhookBaseUrl()) + "/webhook/vapi",
            "headers",
            Map.of(properties.webhookSecretHeader(), properties.webhookSecret())));
    assistant.put("artifactPlan", Map.of("recordingEnabled", true));
    assistant.put("metadata", metadata);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("phoneNumberId", properties.phoneNumberId());
    body.put("customer", Map.of("number", call.phoneNumber(), "name", call.elderlyName()));
    body.put("assistant", assistant);
    body.put("metadata", metadata);
    return body;
  }

  private static JdkClientHttpRequestFactory requestFactory(VapiProperties properties) {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(properties.timeout());
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
