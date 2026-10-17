package br.com.cuida.ligacao;

import br.com.cuida.idoso.AcessoIdosoService;
import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.ligacao.model.LigacaoDTO;
import br.com.cuida.ligacao.model.LigacaoResponseDTO;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LigacaoController {

  private final LigacaoManualService ligacaoManualService;
  private final ProcessamentoLigacaoService processamentoLigacaoService;
  private final AcessoIdosoService acessoIdosoService;

  LigacaoController(
      LigacaoManualService ligacaoManualService,
      ProcessamentoLigacaoService processamentoLigacaoService,
      AcessoIdosoService acessoIdosoService) {
    this.ligacaoManualService = ligacaoManualService;
    this.processamentoLigacaoService = processamentoLigacaoService;
    this.acessoIdosoService = acessoIdosoService;
  }

  /** Dispara uma ligação agora ("ligar agora"). */
  @PostMapping("/api/elderly/{elderlyId}/calls")
  LigacaoResponseDTO callNow(@PathVariable UUID elderlyId) {
    return ligacaoManualService.callNow(elderlyId);
  }

  /** Roda de novo o pipeline (áudio, transcrição, resumo). Só administrador. */
  @PostMapping("/api/calls/{callId}/reprocess")
  LigacaoResponseDTO reprocess(@PathVariable UUID callId) {
    LigacaoDTO call = processamentoLigacaoService.reprocess(callId);
    ZoneId zone =
        acessoIdosoService.find(call.elderlyId()).map(IdosoDTO::zone).orElse(ZoneOffset.UTC);
    return LigacaoResponseDTO.of(call, zone);
  }
}
