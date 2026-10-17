package br.com.cuida.schedule;

import br.com.cuida.elderly.ElderlyRegistered;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública do módulo: agendas ativas para o agendador de ligações. */
@Service
@Transactional(readOnly = true)
public class CallSchedules {

  private final CallScheduleRepository repository;

  CallSchedules(CallScheduleRepository repository) {
    this.repository = repository;
  }

  public List<ScheduleView> findActive() {
    return repository.findByActiveTrue().stream().map(CallSchedule::toView).toList();
  }

  public Optional<ScheduleView> find(UUID elderlyId) {
    return repository.findByElderlyId(elderlyId).map(CallSchedule::toView);
  }

  /** Todo idoso nasce com uma agenda desligada (9h, todos os dias). */
  @EventListener
  @Transactional
  void on(ElderlyRegistered event) {
    if (repository.findByElderlyId(event.elderlyId()).isEmpty()) {
      repository.save(CallSchedule.initialFor(event.elderlyId()));
    }
  }
}
