package br.com.cuida.call.vapi;

import br.com.cuida.call.ProviderCallEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Traduz as mensagens de servidor do Vapi ({@code status-update} e {@code end-of-call-report}) em
 * {@link ProviderCallEvent}. O mock usa o mesmo formato.
 *
 * <p>A transcrição do Vapi vem como linhas {@code "AI: ..."} / {@code "User: ..."}; os rótulos são
 * trocados por {@code "Assistente:"} e {@code "Idoso(a):"} para leitura do tutor.
 */
@Component
public class VapiEventParser {

  private static final Pattern AI_LINE = Pattern.compile("(?m)^(AI|Assistant|assistant|bot):\\s*");
  private static final Pattern USER_LINE = Pattern.compile("(?m)^(User|user|Customer):\\s*");

  private final JsonMapper jsonMapper;

  VapiEventParser(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  public Optional<ProviderCallEvent> parse(String payload) {
    JsonNode message = jsonMapper.readTree(payload).path("message");
    String type = text(message, "type");
    if (type == null) {
      return Optional.empty();
    }
    JsonNode call = message.path("call");
    ProviderCallEvent.CallRef ref =
        new ProviderCallEvent.CallRef(
            uuid(text(call.path("metadata"), "callId")), text(call, "id"));
    return switch (type) {
      case "status-update" ->
          Optional.of(
              new ProviderCallEvent.StatusUpdate(ref, text(message, "status"), timestamp(message)));
      case "end-of-call-report" -> Optional.of(report(ref, message, call));
      default -> Optional.empty();
    };
  }

  private ProviderCallEvent.Report report(
      ProviderCallEvent.CallRef ref, JsonNode message, JsonNode call) {
    JsonNode artifact = message.path("artifact");
    String transcript = first(text(artifact, "transcript"), text(message, "transcript"));
    String recordingUrl =
        first(
            text(artifact, "recordingUrl"),
            text(artifact.path("recording"), "mono", "combinedUrl"),
            text(message, "recordingUrl"));
    Instant startedAt = first(instant(message, "startedAt"), instant(call, "startedAt"));
    Instant endedAt = first(instant(message, "endedAt"), instant(call, "endedAt"));
    Integer duration = null;
    if (message.path("durationSeconds").isNumber()) {
      duration = (int) Math.round(message.path("durationSeconds").asDouble());
    } else if (startedAt != null && endedAt != null) {
      duration = (int) Duration.between(startedAt, endedAt).toSeconds();
    }
    return new ProviderCallEvent.Report(
        ref,
        first(text(message, "endedReason"), text(call, "endedReason")),
        startedAt,
        endedAt,
        duration,
        relabel(transcript),
        recordingUrl);
  }

  static String relabel(String transcript) {
    if (transcript == null) {
      return null;
    }
    String relabeled = AI_LINE.matcher(transcript.trim()).replaceAll("Assistente: ");
    return USER_LINE.matcher(relabeled).replaceAll("Idoso(a): ");
  }

  private static Instant timestamp(JsonNode message) {
    JsonNode timestamp = message.path("timestamp");
    if (timestamp.isNumber()) {
      return Instant.ofEpochMilli(timestamp.asLong());
    }
    return instant(message, "timestamp");
  }

  private static Instant instant(JsonNode node, String field) {
    String value = text(node, field);
    if (value == null) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static String text(JsonNode node, String... path) {
    JsonNode current = node;
    for (String field : path) {
      current = current.path(field);
    }
    if (current.isMissingNode() || current.isNull()) {
      return null;
    }
    String value = current.asString("");
    return value.isBlank() ? null : value;
  }

  private static UUID uuid(String value) {
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @SafeVarargs
  private static <T> T first(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
