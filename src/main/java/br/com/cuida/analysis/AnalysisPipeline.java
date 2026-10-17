package br.com.cuida.analysis;

import br.com.cuida.call.CallEnded;
import br.com.cuida.call.CallProcessing;
import br.com.cuida.call.CallQueries;
import br.com.cuida.call.CallStatus;
import br.com.cuida.call.CallSummary;
import br.com.cuida.call.CallView;
import br.com.cuida.call.TranscriptSource;
import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import br.com.cuida.recording.AudioContent;
import br.com.cuida.recording.Recordings;
import br.com.cuida.recording.StoredRecording;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

/**
 * Pipeline pós-ligação, disparado por {@link CallEnded} (outbox: sobrevive a quedas e é reenviado).
 *
 * <ol>
 *   <li>COMPLETED → PROCESSING;
 *   <li>copia a gravação do provedor para o nosso storage;
 *   <li>transcreve se o modo for {@code WHISPER} ou se o provedor não mandou transcrição;
 *   <li>resume (Structured Output), normaliza e valida → PROCESSED.
 * </ol>
 *
 * Qualquer falha deixa a ligação em {@code PROCESSING_FAILED} com o motivo; o administrador pode
 * reprocessar. Cada etapa é idempotente (áudio já copiado não é copiado de novo).
 */
@Component
class AnalysisPipeline {

  private static final Logger log = LoggerFactory.getLogger(AnalysisPipeline.class);

  private final CallQueries calls;
  private final CallProcessing processing;
  private final Recordings recordings;
  private final Transcriber transcriber;
  private final Summarizer summarizer;
  private final SummaryNormalizer normalizer;
  private final AiResilience resilience;
  private final AiProperties properties;

  AnalysisPipeline(
      CallQueries calls,
      CallProcessing processing,
      Recordings recordings,
      Transcriber transcriber,
      Summarizer summarizer,
      SummaryNormalizer normalizer,
      AiResilience resilience,
      AiProperties properties) {
    this.calls = calls;
    this.processing = processing;
    this.recordings = recordings;
    this.transcriber = transcriber;
    this.summarizer = summarizer;
    this.normalizer = normalizer;
    this.resilience = resilience;
    this.properties = properties;
  }

  @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
  void on(CallEnded event) {
    if (event.status() != CallStatus.COMPLETED || !processing.startProcessing(event.callId())) {
      return;
    }
    try {
      process(calls.find(event.callId()).orElseThrow());
    } catch (RuntimeException e) {
      log.warn("Falha no processamento da ligação {}: {}", event.callId(), e.toString());
      processing.fail(event.callId(), reasonOf(e));
    }
  }

  private void process(CallView call) {
    Optional<String> audioKey = Optional.ofNullable(call.audioObjectKey());
    if (audioKey.isEmpty() && call.providerRecordingUrl() != null) {
      audioKey = copyRecording(call);
    }

    String transcript = call.transcript();
    boolean needsTranscription =
        properties.transcriptionMode() == AiProperties.TranscriptionMode.WHISPER
            || transcript == null
            || transcript.isBlank();
    if (needsTranscription) {
      if (audioKey.isEmpty()) {
        processing.fail(call.id(), "EMPTY_TRANSCRIPT: sem transcrição e sem áudio");
        return;
      }
      byte[] audio = read(audioKey.get());
      String filename = audioKey.get().substring(audioKey.get().lastIndexOf('/') + 1);
      transcript = resilience.call(() -> transcriber.transcribe(audio, filename));
      if (transcript == null || transcript.isBlank()) {
        processing.fail(call.id(), "EMPTY_TRANSCRIPT: transcrição vazia");
        return;
      }
      processing.attachTranscript(call.id(), transcript, TranscriptSource.WHISPER);
    }

    QuestionnaireSnapshot questionnaire =
        call.questionnaireSnapshot() != null
            ? call.questionnaireSnapshot()
            : new QuestionnaireSnapshot(List.of());
    String finalTranscript = transcript;
    CallSummary summary =
        resilience.call(
            () ->
                normalizer.normalize(
                    summarizer.summarize(
                        new SummaryRequest(questionnaire, finalTranscript, call.localDate())),
                    questionnaire,
                    finalTranscript));
    processing.complete(call.id(), summary);
    log.info("Ligação {} processada (alerta {})", call.id(), summary.alertLevel());
  }

  /** Falha ao copiar o áudio não impede o resumo, se já houver transcrição. */
  private Optional<String> copyRecording(CallView call) {
    try {
      StoredRecording stored =
          recordings.storeFromProvider(call.id(), call.elderlyId(), call.providerRecordingUrl());
      processing.attachAudio(
          call.id(), stored.objectKey(), stored.contentType(), stored.sizeBytes());
      return Optional.of(stored.objectKey());
    } catch (RuntimeException e) {
      log.warn("Gravação da ligação {} não pôde ser copiada: {}", call.id(), e.getMessage());
      return Optional.empty();
    }
  }

  private byte[] read(String key) {
    AudioContent content =
        recordings.open(key).orElseThrow(() -> new IllegalStateException("áudio sumiu: " + key));
    try (InputStream in = content.stream()) {
      return in.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String reasonOf(RuntimeException e) {
    String message = e.getMessage() == null ? "" : ": " + e.getMessage();
    return "PIPELINE_ERROR: " + e.getClass().getSimpleName() + message;
  }
}
