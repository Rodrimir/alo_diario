package br.com.cuida.webhook;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retenção (LGPD): o payload de um webhook processado já é apagado na hora; o dos que nunca
 * terminaram (falha definitiva) é apagado depois de {@code app.retention.webhook-payload-days}
 * dias. O registro do evento continua (sem dado pessoal) para a idempotência e para análise.
 */
@Component
class WebhookPayloadRetentionJob {

  private static final Logger log = LoggerFactory.getLogger(WebhookPayloadRetentionJob.class);

  private final WebhookEventRepository repository;
  private final Duration keep;
  private final Clock clock;

  WebhookPayloadRetentionJob(
      WebhookEventRepository repository,
      @Value("${app.retention.webhook-payload-days:7}") int days,
      Clock clock) {
    this.repository = repository;
    this.keep = Duration.ofDays(days);
    this.clock = clock;
  }

  @Scheduled(cron = "0 45 3 * * *")
  @SchedulerLock(name = "retention.webhookPayload")
  void run() {
    purge(clock.instant());
  }

  /**
   * @return quantos payloads foram apagados
   */
  @Transactional
  int purge(Instant now) {
    List<WebhookEvent> old = repository.findUnfinishedWithPayloadReceivedBefore(now.minus(keep));
    old.forEach(WebhookEvent::purgePayload);
    if (!old.isEmpty()) {
      log.info("{} payload(s) de webhook expirado(s) apagado(s)", old.size());
    }
    return old.size();
  }
}
