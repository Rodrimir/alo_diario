package br.com.cuida.call;

import br.com.cuida.elderly.ElderlyDirectory;
import br.com.cuida.elderly.ElderlyView;
import br.com.cuida.questionnaire.Questionnaires;
import br.com.cuida.schedule.CallSchedules;
import br.com.cuida.schedule.ScheduleView;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Agenda a próxima tentativa do dia para ligações automáticas não atendidas, ocupadas ou com falha
 * técnica, respeitando {@code max_attempts} e {@code retry_interval_minutes} da agenda.
 */
@Component
class RetryPlanner {

  private static final Logger log = LoggerFactory.getLogger(RetryPlanner.class);

  private final CallRepository repository;
  private final CallSchedules schedules;
  private final ElderlyDirectory elderlyDirectory;
  private final Questionnaires questionnaires;
  private final TransactionTemplate transactions;

  RetryPlanner(
      CallRepository repository,
      CallSchedules schedules,
      ElderlyDirectory elderlyDirectory,
      Questionnaires questionnaires,
      TransactionTemplate transactions) {
    this.repository = repository;
    this.schedules = schedules;
    this.elderlyDirectory = elderlyDirectory;
    this.questionnaires = questionnaires;
    this.transactions = transactions;
  }

  /**
   * @return ids das retentativas criadas
   */
  List<UUID> planRetries(Instant now) {
    // Dois dias para trás cobre qualquer fuso; o limite real é a janela de recuperação.
    LocalDate since = LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(2);
    List<UUID> created = new ArrayList<>();
    for (Call previous : repository.findRetryCandidates(since)) {
      try {
        planAfter(previous).ifPresent(created::add);
      } catch (DataIntegrityViolationException alreadyPlanned) {
        log.debug("Retentativa da ligação {} já criada", previous.getId());
      }
    }
    return created;
  }

  private Optional<UUID> planAfter(Call previous) {
    if (previous.getStatus() == CallStatus.FAILED
        && !FailureReasons.isRetryable(previous.getFailureReason())) {
      return Optional.empty();
    }
    Optional<ScheduleView> schedule =
        schedules.find(previous.getElderlyId()).filter(ScheduleView::active);
    Optional<ElderlyView> elderly =
        elderlyDirectory
            .find(previous.getElderlyId())
            .filter(e -> e.active() && e.recordingConsent());
    if (schedule.isEmpty()
        || elderly.isEmpty()
        || previous.getAttempt() >= schedule.get().maxAttempts()) {
      return Optional.empty();
    }
    Instant endedAt =
        previous.getEndedAt() != null ? previous.getEndedAt() : previous.getUpdatedAt();
    Instant retryAt = endedAt.plus(Duration.ofMinutes(schedule.get().retryIntervalMinutes()));
    Call retry =
        new Call(
            previous.getElderlyId(),
            CallTrigger.RETRY,
            retryAt,
            previous.getLocalDate(),
            previous.getAttempt() + 1,
            questionnaires.snapshot(previous.getElderlyId()));
    transactions.executeWithoutResult(status -> repository.saveAndFlush(retry));
    log.info(
        "Retentativa {} (tentativa {}) agendada para {}",
        retry.getId(),
        retry.getAttempt(),
        retryAt);
    return Optional.of(retry.getId());
  }
}
