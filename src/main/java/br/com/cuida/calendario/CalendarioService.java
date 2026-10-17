package br.com.cuida.calendario;

import br.com.cuida.auditoria.AuditoriaService;
import br.com.cuida.auditoria.model.AcaoAuditada;
import br.com.cuida.calendario.model.DetalheLigacaoResponseDTO;
import br.com.cuida.calendario.model.DiaCalendarioResponseDTO;
import br.com.cuida.calendario.model.RegistroAcessoResponseDTO;
import br.com.cuida.calendario.model.TranscricaoResponseDTO;
import br.com.cuida.gravacao.GravacaoService;
import br.com.cuida.gravacao.model.ConteudoAudio;
import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.infra.exception.AcessoNegadoException;
import br.com.cuida.infra.exception.RecursoNaoEncontradoException;
import br.com.cuida.infra.exception.RecursoRemovidoException;
import br.com.cuida.infra.security.UsuarioLogado;
import br.com.cuida.infra.security.UsuarioLogadoService;
import br.com.cuida.infra.util.HorarioLocal;
import br.com.cuida.ligacao.ConsultaLigacaoService;
import br.com.cuida.ligacao.FormatadorDeResumo;
import br.com.cuida.ligacao.model.LigacaoDTO;
import br.com.cuida.ligacao.model.LigacaoResponseDTO;
import br.com.cuida.ligacao.model.ResumoFormatadoDTO;
import br.com.cuida.ligacao.model.StatusLigacao;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class CalendarioService {

  private final AcessoIdosoService acessoIdosoService;
  private final FormatadorDeResumo formatadorDeResumo;
  private final ConsultaLigacaoService consultaLigacaoService;
  private final GravacaoService gravacaoService;
  private final AuditoriaService auditoriaService;
  private final UsuarioLogadoService usuarioLogadoService;
  private final Clock clock;

  CalendarioService(
      AcessoIdosoService acessoIdosoService,
      FormatadorDeResumo formatadorDeResumo,
      ConsultaLigacaoService consultaLigacaoService,
      GravacaoService gravacaoService,
      AuditoriaService auditoriaService,
      UsuarioLogadoService usuarioLogadoService,
      Clock clock) {
    this.formatadorDeResumo = formatadorDeResumo;
    this.acessoIdosoService = acessoIdosoService;
    this.consultaLigacaoService = consultaLigacaoService;
    this.gravacaoService = gravacaoService;
    this.auditoriaService = auditoriaService;
    this.usuarioLogadoService = usuarioLogadoService;
    this.clock = clock;
  }

  /** Dias do mês com ao menos uma ligação, em ordem. Mês padrão: o atual no fuso do idoso. */
  List<DiaCalendarioResponseDTO> month(UUID elderlyId, YearMonth month) {
    IdosoDTO elderly = acessoIdosoService.requireAccessible(elderlyId);
    YearMonth target = month != null ? month : YearMonth.now(clock.withZone(elderly.zone()));
    Map<LocalDate, List<LigacaoDTO>> byDay =
        consultaLigacaoService
            .findByElderly(elderlyId, target.atDay(1), target.atEndOfMonth())
            .stream()
            .collect(
                Collectors.groupingBy(LigacaoDTO::localDate, TreeMap::new, Collectors.toList()));
    List<DiaCalendarioResponseDTO> days = new ArrayList<>();
    byDay.forEach(
        (date, dayCalls) -> {
          LigacaoDTO main = mostRelevant(dayCalls);
          days.add(
              new DiaCalendarioResponseDTO(
                  date,
                  main.id(),
                  main.status(),
                  main.alertLevel(),
                  main.mood(),
                  main.summaryText(),
                  dayCalls.size(),
                  main.hasTranscript(),
                  main.hasAudio()));
        });
    return days;
  }

  /** Todas as ligações (tentativas) de um dia. Dia padrão: hoje no fuso do idoso. */
  List<LigacaoResponseDTO> day(UUID elderlyId, LocalDate date) {
    IdosoDTO elderly = acessoIdosoService.requireAccessible(elderlyId);
    LocalDate target = date != null ? date : LocalDate.now(clock.withZone(elderly.zone()));
    return consultaLigacaoService.findByElderly(elderlyId, target, target).stream()
        .map(call -> LigacaoResponseDTO.of(call, elderly.zone()))
        .toList();
  }

  DetalheLigacaoResponseDTO detail(UUID callId, String ip) {
    Access access = requireAccess(callId);
    LigacaoDTO call = access.call();
    ZoneId zone = access.elderly().zone();
    if (call.summary() != null) {
      auditoriaService.record(
          access.userId(), call.elderlyId(), callId, AcaoAuditada.VIEW_SUMMARY, ip);
    }
    ResumoFormatadoDTO formatted =
        call.summary() == null
            ? null
            : formatadorDeResumo.formatar(
                call.summary(), access.elderly().nameForConversation(), call.localDate());
    return new DetalheLigacaoResponseDTO(
        call.id(),
        call.elderlyId(),
        call.trigger(),
        call.status(),
        call.localDate(),
        call.attempt(),
        HorarioLocal.at(call.scheduledFor(), zone),
        HorarioLocal.at(call.startedAt(), zone),
        HorarioLocal.at(call.endedAt(), zone),
        call.durationSeconds(),
        call.endReason(),
        call.failureReason(),
        call.alertLevel(),
        call.summaryText(),
        call.summary(),
        call.hasTranscript(),
        call.hasAudio() ? "/api/calls/" + call.id() + "/audio" : null,
        call.audioPurged(),
        formatted == null ? null : formatted.checklist(),
        formatted == null ? null : formatted.familyMessage(),
        formatted == null ? null : formatted.report());
  }

  TranscricaoResponseDTO transcript(UUID callId, String ip) {
    Access access = requireAccess(callId);
    LigacaoDTO call = access.call();
    if (!call.hasTranscript()) {
      throw new RecursoNaoEncontradoException("Transcrição indisponível para a ligação: " + callId);
    }
    auditoriaService.record(
        access.userId(), call.elderlyId(), callId, AcaoAuditada.VIEW_TRANSCRIPT, ip);
    return new TranscricaoResponseDTO(call.id(), call.transcriptSource(), call.transcript());
  }

  /** 404 se a ligação nunca teve áudio; 410 se a retenção já o apagou. */
  Audio audio(UUID callId, String ip) {
    Access access = requireAccess(callId);
    LigacaoDTO call = access.call();
    if (!call.hasAudio()) {
      if (call.audioPurged()) {
        throw new RecursoRemovidoException(
            "O áudio desta ligação foi apagado pela política de retenção");
      }
      throw new RecursoNaoEncontradoException("Áudio indisponível para a ligação: " + callId);
    }
    ConteudoAudio content =
        gravacaoService
            .open(call.audioObjectKey())
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Áudio indisponível para a ligação: " + callId));
    auditoriaService.record(access.userId(), call.elderlyId(), callId, AcaoAuditada.PLAY_AUDIO, ip);
    String extension = call.audioObjectKey().substring(call.audioObjectKey().lastIndexOf('.'));
    String type = call.audioMimeType() != null ? call.audioMimeType() : content.contentType();
    return new Audio(
        content, type, "ligacao-" + call.localDate() + "-" + call.attempt() + extension);
  }

  /** Log de acessos aos dados do idoso: só administrador. */
  List<RegistroAcessoResponseDTO> accessLog(UUID elderlyId) {
    if (!usuarioLogadoService.require().isAdmin()) {
      throw new AcessoNegadoException("Só administradores consultam o log de acessos");
    }
    IdosoDTO elderly = acessoIdosoService.requireAccessible(elderlyId);
    return auditoriaService.findByElderly(elderlyId).stream()
        .map(
            entry ->
                new RegistroAcessoResponseDTO(
                    entry.id(),
                    entry.userId(),
                    entry.callId(),
                    entry.action(),
                    entry.ip(),
                    HorarioLocal.at(entry.occurredAt(), elderly.zone())))
        .toList();
  }

  /** Áudio pronto para enviar. */
  record Audio(ConteudoAudio content, String contentType, String filename) {}

  private record Access(LigacaoDTO call, IdosoDTO elderly, UUID userId) {}

  /** Ligação de idoso alheio responde 404 de ligação (não revela nem a ligação nem o idoso). */
  private Access requireAccess(UUID callId) {
    UsuarioLogado user = usuarioLogadoService.require();
    RecursoNaoEncontradoException notFound =
        new RecursoNaoEncontradoException("Ligação não encontrada: " + callId);
    LigacaoDTO call = consultaLigacaoService.find(callId).orElseThrow(() -> notFound);
    IdosoDTO elderly =
        acessoIdosoService
            .find(call.elderlyId())
            .filter(e -> user.canAccess(e.tutorId()))
            .orElseThrow(() -> notFound);
    return new Access(call, elderly, user.id());
  }

  private static LigacaoDTO mostRelevant(List<LigacaoDTO> dayCalls) {
    Comparator<LigacaoDTO> byCreation = Comparator.comparing(LigacaoDTO::createdAt);
    return dayCalls.stream()
        .filter(call -> call.status() == StatusLigacao.PROCESSED)
        .max(byCreation)
        .orElseGet(() -> dayCalls.stream().max(byCreation).orElseThrow());
  }
}
