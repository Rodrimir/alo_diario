package br.com.cuida.ligacao.mock;

import br.com.cuida.ligacao.GatewayDeVoz;
import br.com.cuida.ligacao.LigacaoRecusadaException;
import br.com.cuida.ligacao.model.LigacaoIniciada;
import br.com.cuida.ligacao.model.PedidoDeLigacao;
import br.com.cuida.webhook.RecebimentoWebhookService;
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
 * app.voice.mock.delay}, entrega ao {@link RecebimentoWebhookService} os mesmos eventos que o Vapi
 * mandaria ({@code status-update} e {@code end-of-call-report}), no mesmo formato. Assim, o fluxo
 * do webhook em diante é exatamente o de produção.
 */
@Component
@ConditionalOnProperty(name = "app.voice.provider", havingValue = "mock", matchIfMissing = true)
class GatewayDeVozSimulado implements GatewayDeVoz {

  static final String PROVIDER = "mock";
  static final int DURATION_SECONDS = 95;

  private static final Logger log = LoggerFactory.getLogger(GatewayDeVozSimulado.class);

  private final RecebimentoWebhookService recebimentoWebhookService;
  private final JsonMapper jsonMapper;
  private final GatewaySimuladoProperties gatewaySimuladoProperties;
  private final Clock clock;
  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "mock-voice-gateway");
            thread.setDaemon(true);
            return thread;
          });

  GatewayDeVozSimulado(
      RecebimentoWebhookService recebimentoWebhookService,
      JsonMapper jsonMapper,
      GatewaySimuladoProperties gatewaySimuladoProperties,
      Clock clock) {
    this.recebimentoWebhookService = recebimentoWebhookService;
    this.jsonMapper = jsonMapper;
    this.gatewaySimuladoProperties = gatewaySimuladoProperties;
    this.clock = clock;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public LigacaoIniciada start(PedidoDeLigacao call) {
    CenarioSimulado scenario = CenarioSimulado.forPhone(call.phoneNumber());
    if (scenario == CenarioSimulado.DIAL_ERROR) {
      throw new LigacaoRecusadaException("falha simulada ao discar");
    }
    String providerCallId = "mock-" + UUID.randomUUID();
    Instant startedAt = clock.instant();
    executor.schedule(
        () -> deliver(call, providerCallId, scenario, startedAt),
        gatewaySimuladoProperties.delay().toMillis(),
        TimeUnit.MILLISECONDS);
    return new LigacaoIniciada(providerCallId);
  }

  @PreDestroy
  void shutdown() {
    executor.shutdownNow();
  }

  /** A conversa simulada dura {@value #DURATION_SECONDS} s a partir do disparo. */
  private void deliver(
      PedidoDeLigacao call, String providerCallId, CenarioSimulado scenario, Instant startedAt) {
    try {
      Map<String, Object> callNode =
          Map.of("id", providerCallId, "metadata", Map.of("callId", call.callId().toString()));
      boolean answered = scenario == CenarioSimulado.NORMAL || scenario == CenarioSimulado.URGENT;
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
      PedidoDeLigacao call,
      Map<String, Object> callNode,
      CenarioSimulado scenario,
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
                ConversaSimulada.transcript(call, scenario),
                "recordingUrl",
                gatewaySimuladoProperties.recordingUrl()));
      }
    }
    return message;
  }

  private void send(String eventId, String type, Map<String, Object> message) {
    String payload = jsonMapper.writeValueAsString(Map.of("message", message));
    recebimentoWebhookService.receive(PROVIDER, eventId, type, payload);
  }
}
