package br.com.cuida.call;

import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aplica os eventos do provedor às ligações. Idempotente: eventos repetidos não mudam nada. */
@Service
class CallLifecycle {

  /** O que aconteceu com o evento. */
  enum Result {
    APPLIED,
    NO_CHANGE,
    UNKNOWN_CALL
  }

  private final CallRepository repository;
  private final ApplicationEventPublisher events;

  CallLifecycle(CallRepository repository, ApplicationEventPublisher events) {
    this.repository = repository;
    this.events = events;
  }

  @Transactional
  Result apply(ProviderCallEvent event) {
    Optional<Call> found = find(event.ref());
    if (found.isEmpty()) {
      return Result.UNKNOWN_CALL;
    }
    Call call = found.get();
    return switch (event) {
      case ProviderCallEvent.StatusUpdate update -> applyStatus(call, update);
      case ProviderCallEvent.Report report -> applyReport(call, report);
    };
  }

  private Result applyStatus(Call call, ProviderCallEvent.StatusUpdate update) {
    if (!"in-progress".equals(update.status()) || !call.isActive()) {
      return Result.NO_CHANGE;
    }
    call.markInProgress(update.at());
    return Result.APPLIED;
  }

  private Result applyReport(Call call, ProviderCallEvent.Report report) {
    if (!call.acceptsReport()) {
      return Result.NO_CHANGE;
    }
    EndedReasons.Outcome outcome = EndedReasons.outcomeOf(report.endedReason());
    call.recordReport(report, outcome.status(), outcome.failureReason());
    repository.saveAndFlush(call);
    events.publishEvent(new CallEnded(call.getId(), call.getElderlyId(), call.getStatus()));
    return Result.APPLIED;
  }

  private Optional<Call> find(ProviderCallEvent.CallRef ref) {
    if (ref.callId() != null) {
      Optional<Call> byId = repository.findById(ref.callId());
      if (byId.isPresent()) {
        return byId;
      }
    }
    if (ref.providerCallId() != null && !ref.providerCallId().isBlank()) {
      return repository.findByProviderCallId(ref.providerCallId());
    }
    return Optional.empty();
  }
}
