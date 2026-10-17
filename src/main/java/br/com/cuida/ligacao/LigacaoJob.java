package br.com.cuida.ligacao;

import java.time.Clock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Jobs do ciclo de ligações. Cada um roda em uma única instância (ShedLock). */
@Component
class LigacaoJob {

  private final PlanejadorDeLigacoes planejadorDeLigacoes;
  private final PlanejadorDeRetentativas planejadorDeRetentativas;
  private final DisparadorDeLigacoes disparadorDeLigacoes;
  private final VarredorDeLigacoesTravadas varredorDeLigacoesTravadas;
  private final Clock clock;

  LigacaoJob(
      PlanejadorDeLigacoes planejadorDeLigacoes,
      PlanejadorDeRetentativas planejadorDeRetentativas,
      DisparadorDeLigacoes disparadorDeLigacoes,
      VarredorDeLigacoesTravadas varredorDeLigacoesTravadas,
      Clock clock) {
    this.planejadorDeLigacoes = planejadorDeLigacoes;
    this.planejadorDeRetentativas = planejadorDeRetentativas;
    this.disparadorDeLigacoes = disparadorDeLigacoes;
    this.varredorDeLigacoesTravadas = varredorDeLigacoesTravadas;
    this.clock = clock;
  }

  /** A cada minuto: planeja as ligações do dia e as retentativas, e dispara as vencidas. */
  @Scheduled(cron = "0 * * * * *")
  @SchedulerLock(name = "calls.planAndDispatch", lockAtMostFor = "PT5M")
  void planAndDispatch() {
    planejadorDeLigacoes.planDue(clock.instant());
    planejadorDeRetentativas.planRetries(clock.instant());
    disparadorDeLigacoes.dispatchDue(clock.instant());
  }

  @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT2M")
  @SchedulerLock(name = "calls.sweepStuck")
  void sweepStuck() {
    varredorDeLigacoesTravadas.sweep(clock.instant());
  }
}
