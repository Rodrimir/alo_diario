package br.com.cuida.calendario;

import br.com.cuida.calendario.model.DetalheLigacaoResponseDTO;
import br.com.cuida.calendario.model.DiaCalendarioResponseDTO;
import br.com.cuida.calendario.model.RegistroAcessoResponseDTO;
import br.com.cuida.calendario.model.TranscricaoResponseDTO;
import br.com.cuida.infra.exception.ValidacaoException;
import br.com.cuida.infra.util.IpCliente;
import br.com.cuida.ligacao.model.LigacaoResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CalendarioController {

  private final CalendarioService calendarioService;

  CalendarioController(CalendarioService calendarioService) {
    this.calendarioService = calendarioService;
  }

  /** {@code ?month=2026-10} (padrão: mês atual no fuso do idoso). */
  @GetMapping("/api/elderly/{elderlyId}/calendar")
  List<DiaCalendarioResponseDTO> calendar(
      @PathVariable UUID elderlyId, @RequestParam(required = false) String month) {
    return calendarioService.month(elderlyId, parseMonth(month));
  }

  /** {@code ?date=2026-10-17} (padrão: hoje no fuso do idoso). */
  @GetMapping("/api/elderly/{elderlyId}/calls")
  List<LigacaoResponseDTO> day(
      @PathVariable UUID elderlyId, @RequestParam(required = false) String date) {
    return calendarioService.day(elderlyId, parseDate(date));
  }

  @GetMapping("/api/calls/{callId}")
  DetalheLigacaoResponseDTO detail(@PathVariable UUID callId, HttpServletRequest request) {
    return calendarioService.detail(callId, IpCliente.of(request));
  }

  @GetMapping("/api/calls/{callId}/transcript")
  TranscricaoResponseDTO transcript(@PathVariable UUID callId, HttpServletRequest request) {
    return calendarioService.transcript(callId, IpCliente.of(request));
  }

  /**
   * O áudio é servido pela própria API (com o token), nunca por link público: {@code Content-Type}
   * do arquivo e {@code Content-Disposition: inline} para tocar direto no navegador.
   */
  @GetMapping("/api/calls/{callId}/audio")
  ResponseEntity<InputStreamResource> audio(@PathVariable UUID callId, HttpServletRequest request) {
    CalendarioService.Audio audio = calendarioService.audio(callId, IpCliente.of(request));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(audio.contentType()))
        .contentLength(audio.content().contentLength())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(audio.filename(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .body(new SizedStreamResource(audio.content().stream(), audio.content().contentLength()));
  }

  /** Quem acessou resumo, transcrição e áudio do idoso. Só administrador. */
  @GetMapping("/api/elderly/{elderlyId}/access-log")
  List<RegistroAcessoResponseDTO> accessLog(@PathVariable UUID elderlyId) {
    return calendarioService.accessLog(elderlyId);
  }

  /**
   * Stream com tamanho conhecido: o Spring usa o tamanho para responder {@code Range} (206) sem ler
   * o stream duas vezes. Só uma faixa por requisição, que é o que os players pedem.
   */
  static class SizedStreamResource extends InputStreamResource {

    private final long length;

    SizedStreamResource(InputStream stream, long length) {
      super(stream);
      this.length = length;
    }

    @Override
    public long contentLength() {
      return length;
    }
  }

  private static YearMonth parseMonth(String month) {
    if (month == null || month.isBlank()) {
      return null;
    }
    try {
      return YearMonth.parse(month);
    } catch (DateTimeParseException e) {
      throw ValidacaoException.of("month", "use o formato AAAA-MM (ex.: 2026-10)");
    }
  }

  private static LocalDate parseDate(String date) {
    if (date == null || date.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(date);
    } catch (DateTimeParseException e) {
      throw ValidacaoException.of("date", "use o formato AAAA-MM-DD (ex.: 2026-10-17)");
    }
  }
}
