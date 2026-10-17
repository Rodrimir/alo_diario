package br.com.cuida.call;

import br.com.cuida.shared.security.CurrentUserProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * API pública para o pipeline pós-ligação (módulo {@code analysis}) e para a retenção (módulo
 * {@code recording}). Cada método é uma transação curta e idempotente.
 */
@Service
@Transactional
public class CallProcessing {

  private final CallRepository repository;
  private final ApplicationEventPublisher events;
  private final CurrentUserProvider currentUser;

  CallProcessing(
      CallRepository repository,
      ApplicationEventPublisher events,
      CurrentUserProvider currentUser) {
    this.repository = repository;
    this.events = events;
    this.currentUser = currentUser;
  }

  /**
   * @return {@code false} se a ligação não está pronta para processar (ou já foi processada)
   */
  public boolean startProcessing(UUID callId) {
    return repository.findById(callId).map(Call::startProcessing).orElse(false);
  }

  public void attachAudio(UUID callId, String objectKey, String mimeType, long sizeBytes) {
    require(callId).attachAudio(objectKey, mimeType, sizeBytes);
  }

  public void attachTranscript(UUID callId, String transcript, TranscriptSource source) {
    require(callId).attachTranscript(transcript, source);
  }

  public void complete(UUID callId, CallSummary summary) {
    require(callId).markProcessed(summary);
  }

  public void fail(UUID callId, String reason) {
    repository.findById(callId).ifPresent(call -> call.markProcessingFailed(reason));
  }

  /** Ligações com áudio guardado que terminaram antes de {@code before} (retenção). */
  @Transactional(readOnly = true)
  public List<UUID> findWithAudioEndedBefore(Instant before) {
    return repository.findIdsWithAudioEndedBefore(before);
  }

  public void markAudioPurged(UUID callId, Instant at) {
    repository.findById(callId).ifPresent(call -> call.markAudioPurged(at));
  }

  /**
   * Reprocesso manual (só administrador): volta a ligação para {@code COMPLETED} e publica {@link
   * CallEnded} de novo. Útil depois de corrigir uma chave de API ou um prompt.
   */
  CallView reprocess(UUID callId) {
    if (!currentUser.require().isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só administradores reprocessam");
    }
    Call call =
        repository
            .findById(callId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ligação não encontrada: " + callId));
    if (!call.resetForReprocessing()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Só ligações PROCESSED ou PROCESSING_FAILED podem ser reprocessadas");
    }
    repository.saveAndFlush(call);
    events.publishEvent(new CallEnded(call.getId(), call.getElderlyId(), call.getStatus()));
    return call.toView();
  }

  private Call require(UUID callId) {
    return repository
        .findById(callId)
        .orElseThrow(() -> new IllegalStateException("Ligação não encontrada: " + callId));
  }
}
