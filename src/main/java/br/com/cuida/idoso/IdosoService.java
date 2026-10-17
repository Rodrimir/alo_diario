package br.com.cuida.idoso;

import br.com.cuida.idoso.model.Idoso;
import br.com.cuida.idoso.model.IdosoCadastrado;
import br.com.cuida.idoso.model.IdosoExcluido;
import br.com.cuida.idoso.model.IdosoRequestDTO;
import br.com.cuida.idoso.model.IdosoResponseDTO;
import br.com.cuida.infra.exception.ValidacaoException;
import br.com.cuida.infra.security.UsuarioLogado;
import br.com.cuida.infra.security.UsuarioLogadoService;
import br.com.cuida.infra.util.TelefoneNormalizado;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IdosoService {

  static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

  private final IdosoRepository idosoRepository;
  private final UsuarioLogadoService usuarioLogadoService;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  IdosoService(
      IdosoRepository idosoRepository,
      UsuarioLogadoService usuarioLogadoService,
      ApplicationEventPublisher events,
      Clock clock) {
    this.idosoRepository = idosoRepository;
    this.usuarioLogadoService = usuarioLogadoService;
    this.events = events;
    this.clock = clock;
  }

  /** Cadastra para o usuário autenticado (que vira o tutor) e cria agenda/perguntas iniciais. */
  @Transactional
  IdosoResponseDTO create(IdosoRequestDTO request) {
    UsuarioLogado user = usuarioLogadoService.require();
    String phone = phone(request.phoneNumber());
    String timezone = timezone(request.timezone());
    Idoso elderly = new Idoso(user.id(), request.name(), request.preferredName(), phone, timezone);
    elderly.update(
        request.name(),
        request.preferredName(),
        phone,
        timezone,
        request.notes(),
        request.active() == null || request.active());
    elderly.updateGuardian(request.guardianName(), whatsapp(request.guardianWhatsapp()));
    idosoRepository.save(elderly);
    events.publishEvent(new IdosoCadastrado(elderly.getId()));
    return IdosoResponseDTO.of(elderly);
  }

  /** Administrador vê todos; tutor, só os seus. */
  @Transactional(readOnly = true)
  List<IdosoResponseDTO> list() {
    UsuarioLogado user = usuarioLogadoService.require();
    List<Idoso> elderly =
        user.isAdmin()
            ? idosoRepository.findAllByOrderByNameAsc()
            : idosoRepository.findByTutorIdOrderByNameAsc(user.id());
    return elderly.stream().map(IdosoResponseDTO::of).toList();
  }

  @Transactional(readOnly = true)
  IdosoResponseDTO get(UUID id) {
    return IdosoResponseDTO.of(requireAccessible(id));
  }

  @Transactional
  IdosoResponseDTO update(UUID id, IdosoRequestDTO request) {
    Idoso elderly = requireAccessible(id);
    elderly.update(
        request.name(),
        request.preferredName(),
        phone(request.phoneNumber()),
        timezone(request.timezone()),
        request.notes(),
        request.active() == null || request.active());
    elderly.updateGuardian(request.guardianName(), whatsapp(request.guardianWhatsapp()));
    return IdosoResponseDTO.of(idosoRepository.saveAndFlush(elderly));
  }

  @Transactional
  IdosoResponseDTO recordConsent(UUID id, boolean consent) {
    Idoso elderly = requireAccessible(id);
    elderly.recordConsent(consent, usuarioLogadoService.require().id(), clock.instant());
    return IdosoResponseDTO.of(idosoRepository.saveAndFlush(elderly));
  }

  /**
   * Exclusão definitiva (LGPD): o banco apaga em cascata agenda, perguntas e ligações; os áudios
   * são apagados a partir do evento {@link IdosoExcluido}.
   */
  @Transactional
  void delete(UUID id) {
    Idoso elderly = requireAccessible(id);
    idosoRepository.delete(elderly);
    idosoRepository.flush();
    events.publishEvent(new IdosoExcluido(id));
  }

  private Idoso requireAccessible(UUID id) {
    UsuarioLogado user = usuarioLogadoService.require();
    return idosoRepository
        .findById(id)
        .filter(elderly -> user.canAccess(elderly.getTutorId()))
        .orElseThrow(() -> AcessoIdosoService.notFound(id));
  }

  private static String phone(String raw) {
    return TelefoneNormalizado.toE164(raw)
        .orElseThrow(
            () ->
                ValidacaoException.of(
                    "phoneNumber", "telefone inválido: informe DDD + número ou o formato +55..."));
  }

  /** WhatsApp do responsável é opcional; se vier, precisa ser um telefone válido. */
  private static String whatsapp(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return TelefoneNormalizado.toE164(raw)
        .orElseThrow(
            () ->
                ValidacaoException.of(
                    "guardianWhatsapp",
                    "WhatsApp inválido: informe DDD + número ou o formato +55..."));
  }

  private static String timezone(String raw) {
    if (raw == null || raw.isBlank()) {
      return DEFAULT_TIMEZONE;
    }
    try {
      return ZoneId.of(raw.trim()).getId();
    } catch (DateTimeException e) {
      throw ValidacaoException.of("timezone", "fuso inválido (ex.: America/Sao_Paulo)");
    }
  }
}
