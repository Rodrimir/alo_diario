package br.com.cuida.elderly;

import br.com.cuida.shared.security.CurrentUser;
import br.com.cuida.shared.security.CurrentUserProvider;
import br.com.cuida.shared.web.InvalidRequestException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ElderlyService {

  static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

  private final ElderlyRepository repository;
  private final CurrentUserProvider currentUser;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  ElderlyService(
      ElderlyRepository repository,
      CurrentUserProvider currentUser,
      ApplicationEventPublisher events,
      Clock clock) {
    this.repository = repository;
    this.currentUser = currentUser;
    this.events = events;
    this.clock = clock;
  }

  /** Cadastra para o usuário autenticado (que vira o tutor) e cria agenda/perguntas iniciais. */
  @Transactional
  ElderlyResponse create(ElderlyRequest request) {
    CurrentUser user = currentUser.require();
    String phone = phone(request.phoneNumber());
    String timezone = timezone(request.timezone());
    Elderly elderly =
        new Elderly(user.id(), request.name(), request.preferredName(), phone, timezone);
    elderly.update(
        request.name(),
        request.preferredName(),
        phone,
        timezone,
        request.notes(),
        request.active() == null || request.active());
    repository.save(elderly);
    events.publishEvent(new ElderlyRegistered(elderly.getId()));
    return ElderlyResponse.of(elderly);
  }

  /** Administrador vê todos; tutor, só os seus. */
  @Transactional(readOnly = true)
  List<ElderlyResponse> list() {
    CurrentUser user = currentUser.require();
    List<Elderly> elderly =
        user.isAdmin()
            ? repository.findAllByOrderByNameAsc()
            : repository.findByTutorIdOrderByNameAsc(user.id());
    return elderly.stream().map(ElderlyResponse::of).toList();
  }

  @Transactional(readOnly = true)
  ElderlyResponse get(UUID id) {
    return ElderlyResponse.of(requireAccessible(id));
  }

  @Transactional
  ElderlyResponse update(UUID id, ElderlyRequest request) {
    Elderly elderly = requireAccessible(id);
    elderly.update(
        request.name(),
        request.preferredName(),
        phone(request.phoneNumber()),
        timezone(request.timezone()),
        request.notes(),
        request.active() == null || request.active());
    return ElderlyResponse.of(repository.saveAndFlush(elderly));
  }

  @Transactional
  ElderlyResponse recordConsent(UUID id, boolean consent) {
    Elderly elderly = requireAccessible(id);
    elderly.recordConsent(consent, currentUser.require().id(), clock.instant());
    return ElderlyResponse.of(repository.saveAndFlush(elderly));
  }

  /**
   * Exclusão definitiva (LGPD): o banco apaga em cascata agenda, perguntas e ligações; os áudios
   * são apagados a partir do evento {@link ElderlyDeleted}.
   */
  @Transactional
  void delete(UUID id) {
    Elderly elderly = requireAccessible(id);
    repository.delete(elderly);
    repository.flush();
    events.publishEvent(new ElderlyDeleted(id));
  }

  private Elderly requireAccessible(UUID id) {
    CurrentUser user = currentUser.require();
    return repository
        .findById(id)
        .filter(elderly -> user.canAccess(elderly.getTutorId()))
        .orElseThrow(() -> ElderlyDirectory.notFound(id));
  }

  private static String phone(String raw) {
    return PhoneNumbers.toE164(raw)
        .orElseThrow(
            () ->
                InvalidRequestException.of(
                    "phoneNumber", "telefone inválido: informe DDD + número ou o formato +55..."));
  }

  private static String timezone(String raw) {
    if (raw == null || raw.isBlank()) {
      return DEFAULT_TIMEZONE;
    }
    try {
      return ZoneId.of(raw.trim()).getId();
    } catch (DateTimeException e) {
      throw InvalidRequestException.of("timezone", "fuso inválido (ex.: America/Sao_Paulo)");
    }
  }
}
