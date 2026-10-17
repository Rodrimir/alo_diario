package br.com.cuida.call.vapi;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.call.ProviderCallEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class VapiEventParserTest {

  private final VapiEventParser parser = new VapiEventParser(JsonMapper.builder().build());

  @Test
  void parsesEndOfCallReport() {
    UUID callId = UUID.randomUUID();
    String payload =
        """
        {"message": {
          "type": "end-of-call-report",
          "endedReason": "customer-ended-call",
          "startedAt": "2026-10-17T12:00:00.000Z",
          "endedAt": "2026-10-17T12:02:05.500Z",
          "durationSeconds": 125.5,
          "call": {"id": "vapi-123", "metadata": {"callId": "%s"}},
          "artifact": {
            "transcript": "AI: Olá, Dona Maria!\\nUser: Oi, tudo bem?",
            "recordingUrl": "https://storage.vapi.ai/rec.mp3"
          }
        }}
        """
            .formatted(callId);

    ProviderCallEvent event = parser.parse(payload).orElseThrow();

    assertThat(event).isInstanceOf(ProviderCallEvent.Report.class);
    ProviderCallEvent.Report report = (ProviderCallEvent.Report) event;
    assertThat(report.ref().callId()).isEqualTo(callId);
    assertThat(report.ref().providerCallId()).isEqualTo("vapi-123");
    assertThat(report.endedReason()).isEqualTo("customer-ended-call");
    assertThat(report.startedAt()).isEqualTo(Instant.parse("2026-10-17T12:00:00Z"));
    assertThat(report.durationSeconds()).isEqualTo(126);
    assertThat(report.recordingUrl()).isEqualTo("https://storage.vapi.ai/rec.mp3");
    assertThat(report.transcript())
        .isEqualTo("Assistente: Olá, Dona Maria!\nIdoso(a): Oi, tudo bem?");
  }

  @Test
  void parsesStatusUpdateWithEpochTimestamp() {
    String payload =
        """
        {"message": {"type": "status-update", "status": "in-progress", "timestamp": 1792238400000,
                     "call": {"id": "vapi-123"}}}
        """;

    ProviderCallEvent.StatusUpdate update =
        (ProviderCallEvent.StatusUpdate) parser.parse(payload).orElseThrow();

    assertThat(update.status()).isEqualTo("in-progress");
    assertThat(update.at()).isEqualTo(Instant.ofEpochMilli(1792238400000L));
    assertThat(update.ref().callId()).isNull();
  }

  @Test
  void ignoresOtherMessageTypes() {
    assertThat(parser.parse("{\"message\": {\"type\": \"speech-update\"}}")).isEmpty();
  }
}
