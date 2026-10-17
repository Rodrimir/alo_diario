package br.com.cuida.call;

import br.com.cuida.elderly.ElderlyDirectory;
import br.com.cuida.elderly.ElderlyView;
import br.com.cuida.questionnaire.Questionnaires;
import br.com.cuida.schedule.CallSchedules;
import br.com.cuida.schedule.ScheduleView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Cria a primeira tentativa do dia para cada agenda ativa cujo horário (no fuso do idoso) já
 * chegou, dentro da janela de recuperação. Sem consentimento de gravação, não liga: registra o dia
 * como {@code FAILED / RECORDING_CONSENT_MISSING}.
 *
 * <p>O índice único {@code (elderly_id, local_date, attempt)} impede duplicatas mesmo com duas
 * instâncias rodando ao mesmo tempo.
 */
@Component
class CallPlanner {

  private static final Logger log = LoggerFactory.getLogger(CallPlanner.class);

  private final CallSchedules schedules;
  private final ElderlyDirectory elderlyDirectory;
  private final Questionnaires questionnaires;
  private final CallRepository repository;
  private final CallProperties properties;
  private final TransactionTemplate transactions;

  CallPlanner(
      CallSchedules schedules,
      ElderlyDirectory elderlyDirectory,
      Questionnaires questionnaires,
      CallRepository repository,
      CallProperties properties,
      TransactionTemplate transactions) {
    this.schedules = schedules;
    this.elderlyDirectory = elderlyDirectory;
    this.questionnaires = questionnaires;
    this.repository = repository;
    this.properties = properties;
    this.transactions = transactions;
  }

  /**
   * @return ids das ligações criadas
   */
  List<UUID> planDue(Instant now) {
    List<UUID> created = new ArrayList<>();
    for (ScheduleView schedule : schedules.findActive()) {
      try {
        planFor(schedule, now).ifPresent(created::add);
      } catch (DataIntegrityViolationException alreadyPlanned) {
        log.debug("Ligação do idoso {} já planejada por outra instância", schedule.elderlyId());
      } catch (RuntimeException e) {
        log.error("Falha ao planejar a ligação do idoso {}", schedule.elderlyId(), e);
      }
    }
    return created;
  }

  private Optional<UUID> planFor(ScheduleView schedule, Instant now) {
    Optional<ElderlyView> found =
        elderlyDirectory.find(schedule.elderlyId()).filter(ElderlyView::active);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    ElderlyView elderly = found.get();
    ZoneId zone = elderly.zone();
    LocalDate today = LocalDate.ofInstant(now, zone);
    // Ontem também: uma agenda das 23h30 ainda pode ser recuperada logo depois da meia-noite.
    for (LocalDate date : List.of(today.minusDays(1), today)) {
      Optional<Instant> due = schedule.dueAt(date, zone);
      if (due.isEmpty()
          || now.isBefore(due.get())
          || now.isAfter(due.get().plus(properties.recoveryWindow()))
          || repository.existsFirstAutomaticAttempt(elderly.id(), date)) {
        continue;
      }
      Call call =
          elderly.recordingConsent()
              ? new Call(
                  elderly.id(),
                  CallTrigger.SCHEDULED,
                  due.get(),
                  date,
                  1,
                  questionnaires.snapshot(elderly.id()))
              : Call.notPlaced(
                  elderly.id(), due.get(), date, FailureReasons.RECORDING_CONSENT_MISSING);
      transactions.executeWithoutResult(status -> repository.saveAndFlush(call));
      log.info(
          "Ligação {} planejada para o idoso {} em {} ({})",
          call.getId(),
          elderly.id(),
          date,
          call.getStatus());
      return Optional.of(call.getId());
    }
    return Optional.empty();
  }
}
