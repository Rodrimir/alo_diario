package br.com.cuida.ligacao;

import br.com.cuida.infra.exception.RecursoNaoEncontradoException;
import br.com.cuida.ligacao.model.AlertaUrgente;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.RespostaRegistrada;
import br.com.cuida.ligacao.model.RespostaRegistradaDTO;
import br.com.cuida.ligacao.model.StatusResposta;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O que a IA de voz registra durante a ligação (pelas ferramentas MCP): respostas, recado, alerta e
 * o fim da conversa. API pública para os módulos {@code mcp} (escrita) e {@code analise} (leitura).
 */
@Service
@Transactional
public class RespostasDaLigacaoService {

  private final LigacaoRepository ligacaoRepository;
  private final RespostaRegistradaRepository respostaRegistradaRepository;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  RespostasDaLigacaoService(
      LigacaoRepository ligacaoRepository,
      RespostaRegistradaRepository respostaRegistradaRepository,
      ApplicationEventPublisher events,
      Clock clock) {
    this.ligacaoRepository = ligacaoRepository;
    this.respostaRegistradaRepository = respostaRegistradaRepository;
    this.events = events;
    this.clock = clock;
  }

  /** Grava (ou corrige) a resposta de uma pergunta nesta ligação. */
  public void registrar(
      UUID callId, String attributeKey, String question, String answer, StatusResposta status) {
    require(callId);
    Optional<RespostaRegistrada> existente =
        respostaRegistradaRepository.findByCallIdAndAttributeKey(callId, attributeKey);
    if (existente.isPresent()) {
      existente.get().update(question, answer, status, clock.instant());
    } else {
      respostaRegistradaRepository.save(
          new RespostaRegistrada(callId, attributeKey, question, answer, status, clock.instant()));
    }
  }

  /**
   * Registra o alerta; se virou URGENT agora, publica {@link AlertaUrgente} (a família é avisada
   * uma única vez por ligação).
   */
  public void registrarAlerta(UUID callId, NivelAlerta nivel, String motivo) {
    Ligacao ligacao = require(callId);
    if (ligacao.registrarAlerta(nivel, motivo)) {
      events.publishEvent(new AlertaUrgente(callId, ligacao.getElderlyId(), motivo));
    }
  }

  public void concluirConversa(UUID callId) {
    require(callId).concluirConversa(clock.instant());
  }

  @Transactional(readOnly = true)
  public List<RespostaRegistradaDTO> listar(UUID callId) {
    return respostaRegistradaRepository.findByCallIdOrderByRecordedAtAsc(callId).stream()
        .map(RespostaRegistrada::toDTO)
        .toList();
  }

  /** Alerta sinalizado durante a ligação (nível e motivo), se houver. */
  @Transactional(readOnly = true)
  public Optional<AlertaDaLigacao> alerta(UUID callId) {
    return ligacaoRepository
        .findById(callId)
        .filter(l -> l.getRealtimeAlertLevel() != null)
        .map(l -> new AlertaDaLigacao(l.getRealtimeAlertLevel(), l.getRealtimeAlertReason()));
  }

  /** Alerta registrado pela IA durante a ligação. */
  public record AlertaDaLigacao(NivelAlerta nivel, String motivo) {}

  private Ligacao require(UUID callId) {
    return ligacaoRepository
        .findById(callId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Ligação não encontrada: " + callId));
  }
}
