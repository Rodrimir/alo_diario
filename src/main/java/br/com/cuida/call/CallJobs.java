package br.com.cuida.call;

import java.time.Clock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Jobs do ciclo de ligações. Cada um roda em uma única instância (ShedLock). */
@Component
class CallJobs {

  private final CallPlanner planner;
  private final RetryPlanner retryPlanner;
  private final CallDispatcher dispatcher;
  private final StuckCallSweeper sweeper;
  private final Clock clock;

  CallJobs(
      CallPlanner planner,
      RetryPlanner retryPlanner,
      CallDispatcher dispatcher,
      StuckCallSweeper sweeper,
      Clock clock) {
    this.planner = planner;
    this.retryPlanner = retryPlanner;
    this.dispatcher = dispatcher;
    this.sweeper = sweeper;
    this.clock = clock;
  }

  /** A cada minuto: planeja as ligações do dia e as retentativas, e dispara as vencidas. */
  @Scheduled(cron = "0 * * * * *")
  @SchedulerLock(name = "calls.planAndDispatch", lockAtMostFor = "PT5M")
  void planAndDispatch() {
    planner.planDue(clock.instant());
    retryPlanner.planRetries(clock.instant());
    dispatcher.dispatchDue(clock.instant());
  }

  @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT2M")
  @SchedulerLock(name = "calls.sweepStuck")
  void sweepStuck() {
    sweeper.sweep(clock.instant());
  }
}
