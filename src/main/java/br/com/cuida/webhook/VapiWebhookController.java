package br.com.cuida.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Webhook do Vapi ({@code POST /webhook/vapi}, fora de {@code /api}, sem JWT). A autenticidade é
 * garantida por um segredo enviado no header (configurado no assistente transitório), comparado em
 * tempo constante.
 *
 * <p>O Vapi não manda id de evento, então o id é derivado: {@code {callId}:{tipo}[:{status}]}.
 * Responde {@code 200 EVENT_RECEIVED} assim que o evento é gravado; o processamento é assíncrono.
 */
@RestController
@RequestMapping("/webhook")
class VapiWebhookController {

  static final String PROVIDER = "vapi";
  static final Set<String> HANDLED_TYPES = Set.of("status-update", "end-of-call-report");
  static final String RECEIVED = "EVENT_RECEIVED";

  private static final Logger log = LoggerFactory.getLogger(VapiWebhookController.class);

  private final RecebimentoWebhookService recebimentoWebhookService;
  private final VapiWebhookProperties vapiWebhookProperties;
  private final JsonMapper jsonMapper;

  VapiWebhookController(
      RecebimentoWebhookService recebimentoWebhookService,
      VapiWebhookProperties vapiWebhookProperties,
      JsonMapper jsonMapper) {
    this.recebimentoWebhookService = recebimentoWebhookService;
    this.vapiWebhookProperties = vapiWebhookProperties;
    this.jsonMapper = jsonMapper;
  }

  @PostMapping("/vapi")
  ResponseEntity<String> receive(@RequestHeader HttpHeaders headers, @RequestBody String body) {
    if (!authentic(headers.getFirst(vapiWebhookProperties.secretHeader()))) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    JsonNode message;
    try {
      message = jsonMapper.readTree(body).path("message");
    } catch (JacksonException e) {
      return ResponseEntity.badRequest().body("INVALID_JSON");
    }
    String type = message.path("type").asString("");
    if (!HANDLED_TYPES.contains(type)) {
      return ResponseEntity.ok(RECEIVED);
    }
    String callId = message.path("call").path("id").asString("");
    String eventId = callId.isEmpty() ? "sha256:" + sha256(body) : eventId(callId, type, message);
    RecebimentoWebhookService.Result result =
        recebimentoWebhookService.receive(PROVIDER, eventId, type, body);
    log.debug("Webhook vapi {} ({}): {}", type, eventId, result);
    return ResponseEntity.ok(RECEIVED);
  }

  private static String eventId(String callId, String type, JsonNode message) {
    String status = message.path("status").asString("");
    return status.isEmpty() ? callId + ":" + type : callId + ":" + type + ":" + status;
  }

  private boolean authentic(String received) {
    if (!vapiWebhookProperties.configured()) {
      log.warn("Webhook do Vapi recusado: VAPI_WEBHOOK_SECRET não configurado");
      return false;
    }
    if (received == null) {
      return false;
    }
    return MessageDigest.isEqual(
        received.getBytes(StandardCharsets.UTF_8),
        vapiWebhookProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(String body) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
