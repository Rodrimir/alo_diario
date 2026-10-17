package br.com.cuida.webhook;

import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reprocessa os webhooks que falharam (ex.: conflito de concorrência, banco indisponível), até
 * {@value #MAX_ATTEMPTS} tentativas. Depois disso ficam como {@code FAILED} para análise manual.
 */
@Component
class WebhookRetryJob {

  static final int MAX_ATTEMPTS = 5;

  private static final Logger log = LoggerFactory.getLogger(WebhookRetryJob.class);

  private final WebhookEventRepository repository;
  private final ApplicationEventPublisher events;

  WebhookRetryJob(WebhookEventRepository repository, ApplicationEventPublisher events) {
    this.repository = repository;
    this.events = events;
  }

  @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
  @SchedulerLock(name = "webhook.retryFailed")
  void run() {
    retryFailed();
  }

  /**
   * @return quantos eventos voltaram para a fila
   */
  @Transactional
  int retryFailed() {
    List<WebhookEvent> failed = repository.findRetryable(MAX_ATTEMPTS);
    for (WebhookEvent event : failed) {
      event.requeue();
      events.publishEvent(
          new WebhookReceived(event.getId(), event.getProvider(), event.getEventType()));
    }
    if (!failed.isEmpty()) {
      log.info("{} webhook(s) com falha reenviado(s) para processamento", failed.size());
    }
    return failed.size();
  }
}
