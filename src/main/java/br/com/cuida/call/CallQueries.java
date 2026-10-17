package br.com.cuida.call;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de ligações para os outros módulos (API pública). */
@Service
@Transactional(readOnly = true)
public class CallQueries {

  private final CallRepository repository;

  CallQueries(CallRepository repository) {
    this.repository = repository;
  }

  public Optional<CallView> find(UUID callId) {
    return repository.findById(callId).map(Call::toView);
  }

  /** Ligações de um idoso entre duas datas (inclusive), na ordem em que aconteceram. */
  public List<CallView> findByElderly(UUID elderlyId, LocalDate from, LocalDate to) {
    return repository
        .findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscCreatedAtAsc(elderlyId, from, to)
        .stream()
        .map(Call::toView)
        .toList();
  }
}
