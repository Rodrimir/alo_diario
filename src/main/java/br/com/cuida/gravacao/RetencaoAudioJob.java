package br.com.cuida.gravacao;

import br.com.cuida.ligacao.ConsultaLigacaoService;
import br.com.cuida.ligacao.ProcessamentoLigacaoService;
import br.com.cuida.ligacao.model.LigacaoDTO;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retenção (LGPD): apaga os áudios de ligações encerradas há mais de {@code
 * app.retention.audio-days} dias. A ligação fica com {@code audio_purged_at} e o endpoint do áudio
 * passa a responder 410. Transcrição e resumo continuam (prazo deles ainda em aberto).
 */
@Component
class RetencaoAudioJob {

  private static final Logger log = LoggerFactory.getLogger(RetencaoAudioJob.class);

  private final ProcessamentoLigacaoService processamentoLigacaoService;
  private final ConsultaLigacaoService consultaLigacaoService;
  private final ArmazenamentoDeAudio armazenamentoDeAudio;
  private final RetencaoProperties retencaoProperties;
  private final Clock clock;

  RetencaoAudioJob(
      ProcessamentoLigacaoService processamentoLigacaoService,
      ConsultaLigacaoService consultaLigacaoService,
      ArmazenamentoDeAudio armazenamentoDeAudio,
      RetencaoProperties retencaoProperties,
      Clock clock) {
    this.processamentoLigacaoService = processamentoLigacaoService;
    this.consultaLigacaoService = consultaLigacaoService;
    this.armazenamentoDeAudio = armazenamentoDeAudio;
    this.retencaoProperties = retencaoProperties;
    this.clock = clock;
  }

  @Scheduled(cron = "0 15 3 * * *")
  @SchedulerLock(name = "retention.audio", lockAtMostFor = "PT1H")
  void run() {
    purge(clock.instant());
  }

  /**
   * @return quantos áudios foram apagados
   */
  int purge(Instant now) {
    Instant limit = now.minus(Duration.ofDays(retencaoProperties.audioDays()));
    int purged = 0;
    for (UUID callId : processamentoLigacaoService.findWithAudioEndedBefore(limit)) {
      try {
        LigacaoDTO call = consultaLigacaoService.find(callId).orElse(null);
        if (call == null || call.audioObjectKey() == null) {
          continue;
        }
        armazenamentoDeAudio.delete(call.audioObjectKey());
        processamentoLigacaoService.markAudioPurged(callId, now);
        purged++;
      } catch (RuntimeException e) {
        log.error("Falha ao expurgar o áudio da ligação {}", callId, e);
      }
    }
    if (purged > 0) {
      log.info(
          "{} áudio(s) com mais de {} dias apagado(s)", purged, retencaoProperties.audioDays());
    }
    return purged;
  }
}
