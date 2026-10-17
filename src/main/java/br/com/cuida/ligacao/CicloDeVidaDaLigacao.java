package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.EventoDoProvedor;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.LigacaoEncerrada;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aplica os eventos do provedor às ligações. Idempotente: eventos repetidos não mudam nada. */
@Service
class CicloDeVidaDaLigacao {

  /** O que aconteceu com o evento. */
  enum Result {
    APPLIED,
    NO_CHANGE,
    UNKNOWN_CALL
  }

  private final LigacaoRepository ligacaoRepository;
  private final ApplicationEventPublisher events;

  CicloDeVidaDaLigacao(LigacaoRepository ligacaoRepository, ApplicationEventPublisher events) {
    this.ligacaoRepository = ligacaoRepository;
    this.events = events;
  }

  @Transactional
  Result apply(EventoDoProvedor event) {
    Optional<Ligacao> found = find(event.ref());
    if (found.isEmpty()) {
      return Result.UNKNOWN_CALL;
    }
    Ligacao call = found.get();
    return switch (event) {
      case EventoDoProvedor.StatusUpdate update -> applyStatus(call, update);
      case EventoDoProvedor.Report report -> applyReport(call, report);
    };
  }

  private Result applyStatus(Ligacao call, EventoDoProvedor.StatusUpdate update) {
    if (!"in-progress".equals(update.status()) || !call.isActive()) {
      return Result.NO_CHANGE;
    }
    call.markInProgress(update.at());
    return Result.APPLIED;
  }

  private Result applyReport(Ligacao call, EventoDoProvedor.Report report) {
    if (!call.acceptsReport()) {
      return Result.NO_CHANGE;
    }
    MotivosDeEncerramento.Outcome outcome = MotivosDeEncerramento.outcomeOf(report.endedReason());
    call.recordReport(report, outcome.status(), outcome.failureReason());
    ligacaoRepository.saveAndFlush(call);
    events.publishEvent(new LigacaoEncerrada(call.getId(), call.getElderlyId(), call.getStatus()));
    return Result.APPLIED;
  }

  private Optional<Ligacao> find(EventoDoProvedor.CallRef ref) {
    if (ref.callId() != null) {
      Optional<Ligacao> byId = ligacaoRepository.findById(ref.callId());
      if (byId.isPresent()) {
        return byId;
      }
    }
    if (ref.providerCallId() != null && !ref.providerCallId().isBlank()) {
      return ligacaoRepository.findByProviderCallId(ref.providerCallId());
    }
    return Optional.empty();
  }
}
