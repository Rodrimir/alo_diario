package br.com.cuida.recording;

import br.com.cuida.call.CallProcessing;
import br.com.cuida.call.CallQueries;
import br.com.cuida.call.CallView;
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
class AudioRetentionJob {

  private static final Logger log = LoggerFactory.getLogger(AudioRetentionJob.class);

  private final CallProcessing processing;
  private final CallQueries calls;
  private final AudioStorage storage;
  private final RetentionProperties properties;
  private final Clock clock;

  AudioRetentionJob(
      CallProcessing processing,
      CallQueries calls,
      AudioStorage storage,
      RetentionProperties properties,
      Clock clock) {
    this.processing = processing;
    this.calls = calls;
    this.storage = storage;
    this.properties = properties;
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
    Instant limit = now.minus(Duration.ofDays(properties.audioDays()));
    int purged = 0;
    for (UUID callId : processing.findWithAudioEndedBefore(limit)) {
      try {
        CallView call = calls.find(callId).orElse(null);
        if (call == null || call.audioObjectKey() == null) {
          continue;
        }
        storage.delete(call.audioObjectKey());
        processing.markAudioPurged(callId, now);
        purged++;
      } catch (RuntimeException e) {
        log.error("Falha ao expurgar o áudio da ligação {}", callId, e);
      }
    }
    if (purged > 0) {
      log.info("{} áudio(s) com mais de {} dias apagado(s)", purged, properties.audioDays());
    }
    return purged;
  }
}
