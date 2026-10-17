package br.com.cuida.call.mock;

import br.com.cuida.call.OutboundCall;
import br.com.cuida.call.StartedCall;
import br.com.cuida.call.VoiceCallException;
import br.com.cuida.call.VoiceCallGateway;
import br.com.cuida.webhook.WebhookInbox;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gateway de voz simulado (padrão no dev e nos testes). Não liga para ninguém: depois de {@code
 * app.voice.mock.delay}, entrega ao {@link WebhookInbox} os mesmos eventos que o Vapi mandaria
 * ({@code status-update} e {@code end-of-call-report}), no mesmo formato. Assim, o fluxo do webhook
 * em diante é exatamente o de produção.
 */
@Component
@ConditionalOnProperty(name = "app.voice.provider", havingValue = "mock", matchIfMissing = true)
class MockVoiceCallGateway implements VoiceCallGateway {

  static final String PROVIDER = "mock";
  static final int DURATION_SECONDS = 95;

  private static final Logger log = LoggerFactory.getLogger(MockVoiceCallGateway.class);

  private final WebhookInbox inbox;
  private final JsonMapper jsonMapper;
  private final MockVoiceProperties properties;
  private final Clock clock;
  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "mock-voice-gateway");
            thread.setDaemon(true);
            return thread;
          });

  MockVoiceCallGateway(
      WebhookInbox inbox, JsonMapper jsonMapper, MockVoiceProperties properties, Clock clock) {
    this.inbox = inbox;
    this.jsonMapper = jsonMapper;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public StartedCall start(OutboundCall call) {
    MockScenario scenario = MockScenario.forPhone(call.phoneNumber());
    if (scenario == MockScenario.DIAL_ERROR) {
      throw new VoiceCallException("falha simulada ao discar");
    }
    String providerCallId = "mock-" + UUID.randomUUID();
    Instant startedAt = clock.instant();
    executor.schedule(
        () -> deliver(call, providerCallId, scenario, startedAt),
        properties.delay().toMillis(),
        TimeUnit.MILLISECONDS);
    return new StartedCall(providerCallId);
  }

  @PreDestroy
  void shutdown() {
    executor.shutdownNow();
  }

  /** A conversa simulada dura {@value #DURATION_SECONDS} s a partir do disparo. */
  private void deliver(
      OutboundCall call, String providerCallId, MockScenario scenario, Instant startedAt) {
    try {
      Map<String, Object> callNode =
          Map.of("id", providerCallId, "metadata", Map.of("callId", call.callId().toString()));
      boolean answered = scenario == MockScenario.NORMAL || scenario == MockScenario.URGENT;
      Instant endedAt = answered ? startedAt.plusSeconds(DURATION_SECONDS) : startedAt;
      if (answered) {
        send(
            providerCallId + ":status-update:in-progress",
            "status-update",
            Map.of(
                "type",
                "status-update",
                "status",
                "in-progress",
                "timestamp",
                startedAt.toEpochMilli(),
                "call",
                callNode));
      }
      send(
          providerCallId + ":end-of-call-report",
          "end-of-call-report",
          report(call, callNode, scenario, startedAt, endedAt));
    } catch (RuntimeException e) {
      log.error("Falha ao entregar webhook simulado da ligação {}", call.callId(), e);
    }
  }

  private Map<String, Object> report(
      OutboundCall call,
      Map<String, Object> callNode,
      MockScenario scenario,
      Instant startedAt,
      Instant endedAt) {
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("type", "end-of-call-report");
    message.put("call", callNode);
    message.put("endedAt", endedAt.toString());
    switch (scenario) {
      case NO_ANSWER -> {
        message.put("endedReason", "customer-did-not-answer");
        message.put("durationSeconds", 0);
      }
      case BUSY -> {
        message.put("endedReason", "customer-busy");
        message.put("durationSeconds", 0);
      }
      default -> {
        message.put("endedReason", "assistant-ended-call");
        message.put("startedAt", startedAt.toString());
        message.put("durationSeconds", DURATION_SECONDS + 0.4);
        message.put(
            "artifact",
            Map.of(
                "transcript",
                MockConversation.transcript(call, scenario),
                "recordingUrl",
                properties.recordingUrl()));
      }
    }
    return message;
  }

  private void send(String eventId, String type, Map<String, Object> message) {
    String payload = jsonMapper.writeValueAsString(Map.of("message", message));
    inbox.receive(PROVIDER, eventId, type, payload);
  }
}
