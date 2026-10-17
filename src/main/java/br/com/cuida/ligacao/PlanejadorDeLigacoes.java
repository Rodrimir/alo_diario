package br.com.cuida.ligacao;

import br.com.cuida.agenda.AgendaService;
import br.com.cuida.agenda.model.AgendaDTO;
import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.MotivosDeFalha;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.questionario.QuestionarioService;
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
class PlanejadorDeLigacoes {

  private static final Logger log = LoggerFactory.getLogger(PlanejadorDeLigacoes.class);

  private final AgendaService agendaService;
  private final AcessoIdosoService acessoIdosoService;
  private final QuestionarioService questionarioService;
  private final LigacaoRepository ligacaoRepository;
  private final LigacaoProperties ligacaoProperties;
  private final TransactionTemplate transactions;

  PlanejadorDeLigacoes(
      AgendaService agendaService,
      AcessoIdosoService acessoIdosoService,
      QuestionarioService questionarioService,
      LigacaoRepository ligacaoRepository,
      LigacaoProperties ligacaoProperties,
      TransactionTemplate transactions) {
    this.agendaService = agendaService;
    this.acessoIdosoService = acessoIdosoService;
    this.questionarioService = questionarioService;
    this.ligacaoRepository = ligacaoRepository;
    this.ligacaoProperties = ligacaoProperties;
    this.transactions = transactions;
  }

  /**
   * @return ids das ligações criadas
   */
  List<UUID> planDue(Instant now) {
    List<UUID> created = new ArrayList<>();
    for (AgendaDTO schedule : agendaService.findActive()) {
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

  private Optional<UUID> planFor(AgendaDTO schedule, Instant now) {
    Optional<IdosoDTO> found =
        acessoIdosoService.find(schedule.elderlyId()).filter(IdosoDTO::active);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    IdosoDTO elderly = found.get();
    ZoneId zone = elderly.zone();
    LocalDate today = LocalDate.ofInstant(now, zone);
    // Ontem também: uma agenda das 23h30 ainda pode ser recuperada logo depois da meia-noite.
    for (LocalDate date : List.of(today.minusDays(1), today)) {
      Optional<Instant> due = schedule.dueAt(date, zone);
      if (due.isEmpty()
          || now.isBefore(due.get())
          || now.isAfter(due.get().plus(ligacaoProperties.recoveryWindow()))
          || ligacaoRepository.existsFirstAutomaticAttempt(elderly.id(), date)) {
        continue;
      }
      Ligacao call =
          elderly.recordingConsent()
              ? new Ligacao(
                  elderly.id(),
                  OrigemLigacao.SCHEDULED,
                  due.get(),
                  date,
                  1,
                  questionarioService.snapshot(elderly.id()))
              : Ligacao.notPlaced(
                  elderly.id(), due.get(), date, MotivosDeFalha.RECORDING_CONSENT_MISSING);
      transactions.executeWithoutResult(status -> ligacaoRepository.saveAndFlush(call));
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
