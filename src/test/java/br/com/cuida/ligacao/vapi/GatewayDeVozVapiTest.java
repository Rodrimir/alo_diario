package br.com.cuida.ligacao.vapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import br.com.cuida.ligacao.LigacaoRecusadaException;
import br.com.cuida.ligacao.model.PedidoDeLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GatewayDeVozVapiTest {

  static final VapiProperties PROPERTIES =
      new VapiProperties(
          "chave-vapi",
          "numero-123",
          "https://api.vapi.test",
          "https://cuida.exemplo.com/",
          "segredo",
          "X-Webhook-Secret",
          Duration.ofSeconds(5),
          new VapiProperties.Model("openai", "gpt-4o", 0.4),
          new VapiProperties.Voice("azure", "pt-BR-FranciscaNeural"),
          new VapiProperties.Transcritor("deepgram", "nova-2", "pt-BR"));

  MockRestServiceServer server;
  GatewayDeVozVapi gateway;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    gateway = new GatewayDeVozVapi(builder, PROPERTIES);
  }

  @Test
  void createsCallWithTransientAssistantAndWebhookSecret() {
    UUID callId = UUID.randomUUID();
    server
        .expect(requestTo("https://api.vapi.test/call"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer chave-vapi"))
        .andExpect(jsonPath("$.phoneNumberId").value("numero-123"))
        .andExpect(jsonPath("$.customer.number").value("+5511987654321"))
        .andExpect(jsonPath("$.metadata.callId").value(callId.toString()))
        .andExpect(jsonPath("$.assistant.firstMessage").value("Olá, Dona Maria!"))
        .andExpect(jsonPath("$.assistant.model.messages[0].content").value("instruções"))
        .andExpect(jsonPath("$.assistant.transcriber.language").value("pt-BR"))
        .andExpect(
            jsonPath("$.assistant.server.url").value("https://cuida.exemplo.com/webhook/vapi"))
        .andExpect(jsonPath("$.assistant.server.headers.X-Webhook-Secret").value("segredo"))
        .andExpect(jsonPath("$.assistant.serverMessages[1]").value("end-of-call-report"))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"id\": \"vapi-call-1\", \"status\": \"queued\"}"));

    assertThat(gateway.start(call(callId)).providerCallId()).isEqualTo("vapi-call-1");
    server.verify();
  }

  @Test
  void rejectionBecomesVoiceCallException() {
    server.expect(requestTo("https://api.vapi.test/call")).andRespond(withBadRequest());

    assertThatThrownBy(() -> gateway.start(call(UUID.randomUUID())))
        .isInstanceOf(LigacaoRecusadaException.class);
  }

  @Test
  void requiresCredentials() {
    VapiProperties missing =
        new VapiProperties(
            "",
            "n",
            "https://x",
            "https://y",
            "s",
            "h",
            Duration.ofSeconds(1),
            PROPERTIES.model(),
            PROPERTIES.voice(),
            PROPERTIES.transcriber());

    assertThatThrownBy(() -> new GatewayDeVozVapi(RestClient.builder(), missing))
        .hasMessageContaining("VAPI_API_KEY");
  }

  private static PedidoDeLigacao call(UUID callId) {
    return new PedidoDeLigacao(
        callId,
        "+5511987654321",
        "Dona Maria",
        "Olá, Dona Maria!",
        "instruções",
        new QuestionarioSnapshot(List.of()),
        600);
  }
}
