package br.com.cuida.agenda;

import br.com.cuida.agenda.model.Agenda;
import br.com.cuida.agenda.model.AgendaDTO;
import br.com.cuida.agenda.model.AgendaRequestDTO;
import br.com.cuida.agenda.model.AgendaResponseDTO;
import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoCadastrado;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.infra.exception.ValidacaoException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agenda do idoso: leitura e edição pelo tutor, e agendas ativas para o agendador de ligações. */
@Service
@Transactional(readOnly = true)
public class AgendaService {

  private final AgendaRepository agendaRepository;
  private final AcessoIdosoService acessoIdosoService;
  private final Clock clock;

  AgendaService(
      AgendaRepository agendaRepository, AcessoIdosoService acessoIdosoService, Clock clock) {
    this.agendaRepository = agendaRepository;
    this.acessoIdosoService = acessoIdosoService;
    this.clock = clock;
  }

  public List<AgendaDTO> findActive() {
    return agendaRepository.findByActiveTrue().stream().map(Agenda::toDTO).toList();
  }

  public Optional<AgendaDTO> find(UUID elderlyId) {
    return agendaRepository.findByElderlyId(elderlyId).map(Agenda::toDTO);
  }

  @Transactional
  AgendaResponseDTO buscar(UUID elderlyId) {
    IdosoDTO idoso = acessoIdosoService.requireAccessible(elderlyId);
    return toResponse(findOrCreate(elderlyId), idoso);
  }

  @Transactional
  AgendaResponseDTO atualizar(UUID elderlyId, AgendaRequestDTO request) {
    IdosoDTO idoso = acessoIdosoService.requireAccessible(elderlyId);
    if (request.active() && request.daysOfWeek().isEmpty()) {
      throw ValidacaoException.of("daysOfWeek", "informe ao menos um dia para ativar a agenda");
    }
    Agenda agenda = findOrCreate(elderlyId);
    agenda.update(
        request.localTime(),
        request.daysOfWeek(),
        request.active(),
        request.maxAttempts() != null ? request.maxAttempts() : Agenda.DEFAULT_MAX_ATTEMPTS,
        request.retryIntervalMinutes() != null
            ? request.retryIntervalMinutes()
            : Agenda.DEFAULT_RETRY_INTERVAL_MINUTES);
    return toResponse(agendaRepository.saveAndFlush(agenda), idoso);
  }

  /** Todo idoso nasce com uma agenda desligada (9h, todos os dias). */
  @EventListener
  @Transactional
  void on(IdosoCadastrado event) {
    if (agendaRepository.findByElderlyId(event.elderlyId()).isEmpty()) {
      agendaRepository.save(Agenda.initialFor(event.elderlyId()));
    }
  }

  private Agenda findOrCreate(UUID elderlyId) {
    return agendaRepository
        .findByElderlyId(elderlyId)
        .orElseGet(() -> agendaRepository.save(Agenda.initialFor(elderlyId)));
  }

  private AgendaResponseDTO toResponse(Agenda agenda, IdosoDTO idoso) {
    LocalDateTime next =
        agenda
            .toDTO()
            .nextOccurrence(clock.instant(), idoso.zone())
            .map(ZonedDateTime::toLocalDateTime)
            .orElse(null);
    return new AgendaResponseDTO(
        agenda.getElderlyId(),
        agenda.getLocalTime(),
        agenda.getDaysOfWeek().stream().sorted().toList(),
        agenda.isActive(),
        agenda.getMaxAttempts(),
        agenda.getRetryIntervalMinutes(),
        next);
  }
}
