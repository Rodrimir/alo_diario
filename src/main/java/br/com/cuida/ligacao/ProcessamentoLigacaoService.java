package br.com.cuida.ligacao;

import br.com.cuida.infra.exception.AcessoNegadoException;
import br.com.cuida.infra.exception.ConflitoException;
import br.com.cuida.infra.exception.RecursoNaoEncontradoException;
import br.com.cuida.infra.security.UsuarioLogadoService;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.LigacaoDTO;
import br.com.cuida.ligacao.model.LigacaoEncerrada;
import br.com.cuida.ligacao.model.LigacaoProcessada;
import br.com.cuida.ligacao.model.OrigemTranscricao;
import br.com.cuida.ligacao.model.ResumoLigacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API pública para o pipeline pós-ligação (módulo {@code analysis}) e para a retenção (módulo
 * {@code recording}). Cada método é uma transação curta e idempotente.
 */
@Service
@Transactional
public class ProcessamentoLigacaoService {

  private final LigacaoRepository ligacaoRepository;
  private final ApplicationEventPublisher events;
  private final UsuarioLogadoService usuarioLogadoService;

  ProcessamentoLigacaoService(
      LigacaoRepository ligacaoRepository,
      ApplicationEventPublisher events,
      UsuarioLogadoService usuarioLogadoService) {
    this.ligacaoRepository = ligacaoRepository;
    this.events = events;
    this.usuarioLogadoService = usuarioLogadoService;
  }

  /**
   * @return {@code false} se a ligação não está pronta para processar (ou já foi processada)
   */
  public boolean startProcessing(UUID callId) {
    return ligacaoRepository.findById(callId).map(Ligacao::startProcessing).orElse(false);
  }

  public void attachAudio(UUID callId, String objectKey, String mimeType, long sizeBytes) {
    require(callId).attachAudio(objectKey, mimeType, sizeBytes);
  }

  public void attachTranscript(UUID callId, String transcript, OrigemTranscricao source) {
    require(callId).attachTranscript(transcript, source);
  }

  /** Grava o resumo (PROCESSED) e publica {@link LigacaoProcessada} para avisar a família. */
  public void complete(UUID callId, ResumoLigacao summary) {
    Ligacao call = require(callId);
    call.markProcessed(summary);
    events.publishEvent(new LigacaoProcessada(call.getId(), call.getElderlyId()));
  }

  public void fail(UUID callId, String reason) {
    ligacaoRepository.findById(callId).ifPresent(call -> call.markProcessingFailed(reason));
  }

  /** Ligações com áudio guardado que terminaram antes de {@code before} (retenção). */
  @Transactional(readOnly = true)
  public List<UUID> findWithAudioEndedBefore(Instant before) {
    return ligacaoRepository.findIdsWithAudioEndedBefore(before);
  }

  public void markAudioPurged(UUID callId, Instant at) {
    ligacaoRepository.findById(callId).ifPresent(call -> call.markAudioPurged(at));
  }

  /**
   * Reprocesso manual (só administrador): volta a ligação para {@code COMPLETED} e publica {@link
   * LigacaoEncerrada} de novo. Útil depois de corrigir uma chave de API ou um prompt.
   */
  LigacaoDTO reprocess(UUID callId) {
    if (!usuarioLogadoService.require().isAdmin()) {
      throw new AcessoNegadoException("Só administradores reprocessam");
    }
    Ligacao call =
        ligacaoRepository
            .findById(callId)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Ligação não encontrada: " + callId));
    if (!call.resetForReprocessing()) {
      throw new ConflitoException(
          "Só ligações PROCESSED ou PROCESSING_FAILED podem ser reprocessadas");
    }
    ligacaoRepository.saveAndFlush(call);
    events.publishEvent(new LigacaoEncerrada(call.getId(), call.getElderlyId(), call.getStatus()));
    return call.toDTO();
  }

  private Ligacao require(UUID callId) {
    return ligacaoRepository
        .findById(callId)
        .orElseThrow(() -> new IllegalStateException("Ligação não encontrada: " + callId));
  }
}
