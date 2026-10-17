package br.com.cuida.schedule;

import br.com.cuida.elderly.ElderlyDirectory;
import br.com.cuida.elderly.ElderlyView;
import br.com.cuida.shared.web.InvalidRequestException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elderly/{elderlyId}/schedule")
class ScheduleController {

  private final CallScheduleRepository repository;
  private final ElderlyDirectory elderlyDirectory;
  private final Clock clock;

  ScheduleController(
      CallScheduleRepository repository, ElderlyDirectory elderlyDirectory, Clock clock) {
    this.repository = repository;
    this.elderlyDirectory = elderlyDirectory;
    this.clock = clock;
  }

  @GetMapping
  @Transactional
  ScheduleResponse get(@PathVariable UUID elderlyId) {
    ElderlyView elderly = elderlyDirectory.requireAccessible(elderlyId);
    return toResponse(findOrCreate(elderlyId), elderly);
  }

  @PutMapping
  @Transactional
  ScheduleResponse update(
      @PathVariable UUID elderlyId, @Valid @RequestBody ScheduleRequest request) {
    ElderlyView elderly = elderlyDirectory.requireAccessible(elderlyId);
    if (request.active() && request.daysOfWeek().isEmpty()) {
      throw InvalidRequestException.of(
          "daysOfWeek", "informe ao menos um dia para ativar a agenda");
    }
    CallSchedule schedule = findOrCreate(elderlyId);
    schedule.update(
        request.localTime(),
        request.daysOfWeek(),
        request.active(),
        request.maxAttempts() != null ? request.maxAttempts() : CallSchedule.DEFAULT_MAX_ATTEMPTS,
        request.retryIntervalMinutes() != null
            ? request.retryIntervalMinutes()
            : CallSchedule.DEFAULT_RETRY_INTERVAL_MINUTES);
    return toResponse(repository.saveAndFlush(schedule), elderly);
  }

  private CallSchedule findOrCreate(UUID elderlyId) {
    return repository
        .findByElderlyId(elderlyId)
        .orElseGet(() -> repository.save(CallSchedule.initialFor(elderlyId)));
  }

  private ScheduleResponse toResponse(CallSchedule schedule, ElderlyView elderly) {
    ScheduleView view = schedule.toView();
    LocalDateTime next =
        view.nextOccurrence(clock.instant(), elderly.zone())
            .map(ZonedDateTime::toLocalDateTime)
            .orElse(null);
    return new ScheduleResponse(
        schedule.getElderlyId(),
        schedule.getLocalTime(),
        schedule.getDaysOfWeek().stream().sorted().toList(),
        schedule.isActive(),
        schedule.getMaxAttempts(),
        schedule.getRetryIntervalMinutes(),
        next);
  }
}
