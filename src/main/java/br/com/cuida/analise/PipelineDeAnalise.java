package br.com.cuida.analise;

import br.com.cuida.analise.model.PedidoDeResumo;
import br.com.cuida.gravacao.GravacaoService;
import br.com.cuida.gravacao.model.ConteudoAudio;
import br.com.cuida.gravacao.model.GravacaoArmazenada;
import br.com.cuida.ligacao.ConsultaLigacaoService;
import br.com.cuida.ligacao.ProcessamentoLigacaoService;
import br.com.cuida.ligacao.model.LigacaoDTO;
import br.com.cuida.ligacao.model.LigacaoEncerrada;
import br.com.cuida.ligacao.model.OrigemTranscricao;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
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
 * Pipeline pós-ligação, disparado por {@link LigacaoEncerrada} (outbox: sobrevive a quedas e é
 * reenviado).
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
class PipelineDeAnalise {

  private static final Logger log = LoggerFactory.getLogger(PipelineDeAnalise.class);

  private final ConsultaLigacaoService consultaLigacaoService;
  private final ProcessamentoLigacaoService processamentoLigacaoService;
  private final GravacaoService gravacaoService;
  private final Transcritor transcritor;
  private final Resumidor resumidor;
  private final NormalizadorDeResumo normalizadorDeResumo;
  private final ResilienciaIa resilienciaIa;
  private final IaProperties iaProperties;

  PipelineDeAnalise(
      ConsultaLigacaoService consultaLigacaoService,
      ProcessamentoLigacaoService processamentoLigacaoService,
      GravacaoService gravacaoService,
      Transcritor transcritor,
      Resumidor resumidor,
      NormalizadorDeResumo normalizadorDeResumo,
      ResilienciaIa resilienciaIa,
      IaProperties iaProperties) {
    this.consultaLigacaoService = consultaLigacaoService;
    this.processamentoLigacaoService = processamentoLigacaoService;
    this.gravacaoService = gravacaoService;
    this.transcritor = transcritor;
    this.resumidor = resumidor;
    this.normalizadorDeResumo = normalizadorDeResumo;
    this.resilienciaIa = resilienciaIa;
    this.iaProperties = iaProperties;
  }

  @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
  void on(LigacaoEncerrada event) {
    if (event.status() != StatusLigacao.COMPLETED
        || !processamentoLigacaoService.startProcessing(event.callId())) {
      return;
    }
    try {
      process(consultaLigacaoService.find(event.callId()).orElseThrow());
    } catch (RuntimeException e) {
      log.warn("Falha no processamento da ligação {}: {}", event.callId(), e.toString());
      processamentoLigacaoService.fail(event.callId(), reasonOf(e));
    }
  }

  private void process(LigacaoDTO call) {
    Optional<String> audioKey = Optional.ofNullable(call.audioObjectKey());
    if (audioKey.isEmpty() && call.providerRecordingUrl() != null) {
      audioKey = copyRecording(call);
    }

    String transcript = call.transcript();
    boolean needsTranscription =
        iaProperties.transcriptionMode() == IaProperties.TranscriptionMode.WHISPER
            || transcript == null
            || transcript.isBlank();
    if (needsTranscription) {
      if (audioKey.isEmpty()) {
        processamentoLigacaoService.fail(
            call.id(), "EMPTY_TRANSCRIPT: sem transcrição e sem áudio");
        return;
      }
      byte[] audio = read(audioKey.get());
      String filename = audioKey.get().substring(audioKey.get().lastIndexOf('/') + 1);
      transcript = resilienciaIa.call(() -> transcritor.transcribe(audio, filename));
      if (transcript == null || transcript.isBlank()) {
        processamentoLigacaoService.fail(call.id(), "EMPTY_TRANSCRIPT: transcrição vazia");
        return;
      }
      processamentoLigacaoService.attachTranscript(
          call.id(), transcript, OrigemTranscricao.WHISPER);
    }

    QuestionarioSnapshot questionnaire =
        call.questionnaireSnapshot() != null
            ? call.questionnaireSnapshot()
            : new QuestionarioSnapshot(List.of());
    String finalTranscript = transcript;
    ResumoLigacao summary =
        resilienciaIa.call(
            () ->
                normalizadorDeResumo.normalize(
                    resumidor.summarize(
                        new PedidoDeResumo(questionnaire, finalTranscript, call.localDate())),
                    questionnaire,
                    finalTranscript));
    processamentoLigacaoService.complete(call.id(), summary);
    log.info("Ligação {} processada (alerta {})", call.id(), summary.alertLevel());
  }

  /** Falha ao copiar o áudio não impede o resumo, se já houver transcrição. */
  private Optional<String> copyRecording(LigacaoDTO call) {
    try {
      GravacaoArmazenada stored =
          gravacaoService.storeFromProvider(
              call.id(), call.elderlyId(), call.providerRecordingUrl());
      processamentoLigacaoService.attachAudio(
          call.id(), stored.objectKey(), stored.contentType(), stored.sizeBytes());
      return Optional.of(stored.objectKey());
    } catch (RuntimeException e) {
      log.warn("Gravação da ligação {} não pôde ser copiada: {}", call.id(), e.getMessage());
      return Optional.empty();
    }
  }

  private byte[] read(String key) {
    ConteudoAudio content =
        gravacaoService
            .open(key)
            .orElseThrow(() -> new IllegalStateException("áudio sumiu: " + key));
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
