package br.com.cuida.infra.eventos;

import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Manutenção do outbox de eventos ({@code event_publication}): reenvia publicações que ficaram
 * pendentes (listener falhou ou a aplicação caiu) e apaga as concluídas antigas.
 */
@Component
class ManutencaoDeEventosJob {

  private static final Logger log = LoggerFactory.getLogger(ManutencaoDeEventosJob.class);

  private static final Duration RESUBMIT_AFTER = Duration.ofMinutes(10);
  private static final Duration KEEP_COMPLETED = Duration.ofDays(7);

  private final IncompleteEventPublications incomplete;
  private final CompletedEventPublications completed;

  ManutencaoDeEventosJob(
      IncompleteEventPublications incomplete, CompletedEventPublications completed) {
    this.incomplete = incomplete;
    this.completed = completed;
  }

  @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
  @SchedulerLock(name = "events.resubmitIncomplete")
  void resubmitIncomplete() {
    incomplete.resubmitIncompletePublicationsOlderThan(RESUBMIT_AFTER);
  }

  @Scheduled(cron = "0 30 3 * * *")
  @SchedulerLock(name = "events.deleteCompleted")
  void deleteCompleted() {
    completed.deletePublicationsOlderThan(KEEP_COMPLETED);
    log.debug("Publicações de eventos concluídas há mais de {} apagadas", KEEP_COMPLETED);
  }
}
