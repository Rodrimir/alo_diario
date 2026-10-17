package br.com.cuida.notificacao;

import br.com.cuida.notificacao.model.Notificacao;
import br.com.cuida.notificacao.model.StatusNotificacao;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reenvia os avisos que falharam (whats-api fora do ar, timeout), até {@value #MAX_TENTATIVAS}
 * vezes.
 */
@Component
class ReenvioNotificacaoJob {

  static final int MAX_TENTATIVAS = 3;

  private static final Logger log = LoggerFactory.getLogger(ReenvioNotificacaoJob.class);

  private final NotificacaoRepository notificacaoRepository;
  private final NotificacaoService notificacaoService;

  ReenvioNotificacaoJob(
      NotificacaoRepository notificacaoRepository, NotificacaoService notificacaoService) {
    this.notificacaoRepository = notificacaoRepository;
    this.notificacaoService = notificacaoService;
  }

  @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
  @SchedulerLock(name = "notificacoes.reenviar")
  void run() {
    reenviarFalhas();
  }

  /**
   * @return quantos avisos foram tentados de novo
   */
  int reenviarFalhas() {
    var falhas =
        notificacaoRepository.findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
            StatusNotificacao.FAILED, MAX_TENTATIVAS);
    for (Notificacao falha : falhas) {
      notificacaoService.reenviarFalha(falha.getId());
    }
    if (!falhas.isEmpty()) {
      log.info("{} aviso(s) pelo WhatsApp tentado(s) de novo", falhas.size());
    }
    return falhas.size();
  }
}
