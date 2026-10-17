package br.com.cuida.ligacao;

import br.com.cuida.agenda.AgendaService;
import br.com.cuida.agenda.model.AgendaDTO;
import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.MotivosDeFalha;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.ligacao.model.StatusLigacao;
import br.com.cuida.questionario.QuestionarioService;
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
class PlanejadorDeRetentativas {

  private static final Logger log = LoggerFactory.getLogger(PlanejadorDeRetentativas.class);

  private final LigacaoRepository ligacaoRepository;
  private final AgendaService agendaService;
  private final AcessoIdosoService acessoIdosoService;
  private final QuestionarioService questionarioService;
  private final TransactionTemplate transactions;

  PlanejadorDeRetentativas(
      LigacaoRepository ligacaoRepository,
      AgendaService agendaService,
      AcessoIdosoService acessoIdosoService,
      QuestionarioService questionarioService,
      TransactionTemplate transactions) {
    this.ligacaoRepository = ligacaoRepository;
    this.agendaService = agendaService;
    this.acessoIdosoService = acessoIdosoService;
    this.questionarioService = questionarioService;
    this.transactions = transactions;
  }

  /**
   * @return ids das retentativas criadas
   */
  List<UUID> planRetries(Instant now) {
    // Dois dias para trás cobre qualquer fuso; o limite real é a janela de recuperação.
    LocalDate since = LocalDate.ofInstant(now, ZoneOffset.UTC).minusDays(2);
    List<UUID> created = new ArrayList<>();
    for (Ligacao previous : ligacaoRepository.findRetryCandidates(since)) {
      try {
        planAfter(previous).ifPresent(created::add);
      } catch (DataIntegrityViolationException alreadyPlanned) {
        log.debug("Retentativa da ligação {} já criada", previous.getId());
      }
    }
    return created;
  }

  private Optional<UUID> planAfter(Ligacao previous) {
    if (previous.getStatus() == StatusLigacao.FAILED
        && !MotivosDeFalha.isRetryable(previous.getFailureReason())) {
      return Optional.empty();
    }
    Optional<AgendaDTO> schedule =
        agendaService.find(previous.getElderlyId()).filter(AgendaDTO::active);
    Optional<IdosoDTO> elderly =
        acessoIdosoService
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
    Ligacao retry =
        new Ligacao(
            previous.getElderlyId(),
            OrigemLigacao.RETRY,
            retryAt,
            previous.getLocalDate(),
            previous.getAttempt() + 1,
            questionarioService.snapshot(previous.getElderlyId()));
    transactions.executeWithoutResult(status -> ligacaoRepository.saveAndFlush(retry));
    log.info(
        "Retentativa {} (tentativa {}) agendada para {}",
        retry.getId(),
        retry.getAttempt(),
        retryAt);
    return Optional.of(retry.getId());
  }
}
