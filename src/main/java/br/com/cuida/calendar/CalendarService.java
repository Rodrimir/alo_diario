package br.com.cuida.calendar;

import br.com.cuida.audit.AuditAction;
import br.com.cuida.audit.AuditTrail;
import br.com.cuida.call.CallQueries;
import br.com.cuida.call.CallResponse;
import br.com.cuida.call.CallStatus;
import br.com.cuida.call.CallView;
import br.com.cuida.elderly.ElderlyDirectory;
import br.com.cuida.elderly.ElderlyView;
import br.com.cuida.recording.AudioContent;
import br.com.cuida.recording.Recordings;
import br.com.cuida.shared.security.CurrentUser;
import br.com.cuida.shared.security.CurrentUserProvider;
import br.com.cuida.shared.time.LocalTimes;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class CalendarService {

  private final ElderlyDirectory elderlyDirectory;
  private final CallQueries calls;
  private final Recordings recordings;
  private final AuditTrail auditTrail;
  private final CurrentUserProvider currentUser;
  private final Clock clock;

  CalendarService(
      ElderlyDirectory elderlyDirectory,
      CallQueries calls,
      Recordings recordings,
      AuditTrail auditTrail,
      CurrentUserProvider currentUser,
      Clock clock) {
    this.elderlyDirectory = elderlyDirectory;
    this.calls = calls;
    this.recordings = recordings;
    this.auditTrail = auditTrail;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /** Dias do mês com ao menos uma ligação, em ordem. Mês padrão: o atual no fuso do idoso. */
  List<CalendarDayResponse> month(UUID elderlyId, YearMonth month) {
    ElderlyView elderly = elderlyDirectory.requireAccessible(elderlyId);
    YearMonth target = month != null ? month : YearMonth.now(clock.withZone(elderly.zone()));
    Map<LocalDate, List<CallView>> byDay =
        calls.findByElderly(elderlyId, target.atDay(1), target.atEndOfMonth()).stream()
            .collect(Collectors.groupingBy(CallView::localDate, TreeMap::new, Collectors.toList()));
    List<CalendarDayResponse> days = new ArrayList<>();
    byDay.forEach(
        (date, dayCalls) -> {
          CallView main = mostRelevant(dayCalls);
          days.add(
              new CalendarDayResponse(
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
  List<CallResponse> day(UUID elderlyId, LocalDate date) {
    ElderlyView elderly = elderlyDirectory.requireAccessible(elderlyId);
    LocalDate target = date != null ? date : LocalDate.now(clock.withZone(elderly.zone()));
    return calls.findByElderly(elderlyId, target, target).stream()
        .map(call -> CallResponse.of(call, elderly.zone()))
        .toList();
  }

  CallDetailResponse detail(UUID callId, String ip) {
    Access access = requireAccess(callId);
    CallView call = access.call();
    ZoneId zone = access.elderly().zone();
    if (call.summary() != null) {
      auditTrail.record(access.userId(), call.elderlyId(), callId, AuditAction.VIEW_SUMMARY, ip);
    }
    return new CallDetailResponse(
        call.id(),
        call.elderlyId(),
        call.trigger(),
        call.status(),
        call.localDate(),
        call.attempt(),
        LocalTimes.at(call.scheduledFor(), zone),
        LocalTimes.at(call.startedAt(), zone),
        LocalTimes.at(call.endedAt(), zone),
        call.durationSeconds(),
        call.endReason(),
        call.failureReason(),
        call.alertLevel(),
        call.summaryText(),
        call.summary(),
        call.hasTranscript(),
        call.hasAudio() ? "/api/calls/" + call.id() + "/audio" : null,
        call.audioPurged());
  }

  TranscriptResponse transcript(UUID callId, String ip) {
    Access access = requireAccess(callId);
    CallView call = access.call();
    if (!call.hasTranscript()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Transcrição indisponível para a ligação: " + callId);
    }
    auditTrail.record(access.userId(), call.elderlyId(), callId, AuditAction.VIEW_TRANSCRIPT, ip);
    return new TranscriptResponse(call.id(), call.transcriptSource(), call.transcript());
  }

  /** 404 se a ligação nunca teve áudio; 410 se a retenção já o apagou. */
  Audio audio(UUID callId, String ip) {
    Access access = requireAccess(callId);
    CallView call = access.call();
    if (!call.hasAudio()) {
      if (call.audioPurged()) {
        throw new ResponseStatusException(
            HttpStatus.GONE, "O áudio desta ligação foi apagado pela política de retenção");
      }
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Áudio indisponível para a ligação: " + callId);
    }
    AudioContent content =
        recordings
            .open(call.audioObjectKey())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Áudio indisponível para a ligação: " + callId));
    auditTrail.record(access.userId(), call.elderlyId(), callId, AuditAction.PLAY_AUDIO, ip);
    String extension = call.audioObjectKey().substring(call.audioObjectKey().lastIndexOf('.'));
    String type = call.audioMimeType() != null ? call.audioMimeType() : content.contentType();
    return new Audio(
        content, type, "ligacao-" + call.localDate() + "-" + call.attempt() + extension);
  }

  /** Log de acessos aos dados do idoso: só administrador. */
  List<AccessLogEntryResponse> accessLog(UUID elderlyId) {
    if (!currentUser.require().isAdmin()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Só administradores consultam o log de acessos");
    }
    ElderlyView elderly = elderlyDirectory.requireAccessible(elderlyId);
    return auditTrail.findByElderly(elderlyId).stream()
        .map(
            entry ->
                new AccessLogEntryResponse(
                    entry.id(),
                    entry.userId(),
                    entry.callId(),
                    entry.action(),
                    entry.ip(),
                    LocalTimes.at(entry.occurredAt(), elderly.zone())))
        .toList();
  }

  /** Áudio pronto para enviar. */
  record Audio(AudioContent content, String contentType, String filename) {}

  private record Access(CallView call, ElderlyView elderly, UUID userId) {}

  /** Ligação de idoso alheio responde 404 de ligação (não revela nem a ligação nem o idoso). */
  private Access requireAccess(UUID callId) {
    CurrentUser user = currentUser.require();
    ResponseStatusException notFound =
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Ligação não encontrada: " + callId);
    CallView call = calls.find(callId).orElseThrow(() -> notFound);
    ElderlyView elderly =
        elderlyDirectory
            .find(call.elderlyId())
            .filter(e -> user.canAccess(e.tutorId()))
            .orElseThrow(() -> notFound);
    return new Access(call, elderly, user.id());
  }

  private static CallView mostRelevant(List<CallView> dayCalls) {
    Comparator<CallView> byCreation = Comparator.comparing(CallView::createdAt);
    return dayCalls.stream()
        .filter(call -> call.status() == CallStatus.PROCESSED)
        .max(byCreation)
        .orElseGet(() -> dayCalls.stream().max(byCreation).orElseThrow());
  }
}
