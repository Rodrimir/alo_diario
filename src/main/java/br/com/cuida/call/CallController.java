package br.com.cuida.call;

import br.com.cuida.elderly.ElderlyDirectory;
import br.com.cuida.elderly.ElderlyView;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CallController {

  private final ManualCallService manualCalls;
  private final CallProcessing processing;
  private final ElderlyDirectory elderlyDirectory;

  CallController(
      ManualCallService manualCalls, CallProcessing processing, ElderlyDirectory elderlyDirectory) {
    this.manualCalls = manualCalls;
    this.processing = processing;
    this.elderlyDirectory = elderlyDirectory;
  }

  /** Dispara uma ligação agora ("ligar agora"). */
  @PostMapping("/api/elderly/{elderlyId}/calls")
  CallResponse callNow(@PathVariable UUID elderlyId) {
    return manualCalls.callNow(elderlyId);
  }

  /** Roda de novo o pipeline (áudio, transcrição, resumo). Só administrador. */
  @PostMapping("/api/calls/{callId}/reprocess")
  CallResponse reprocess(@PathVariable UUID callId) {
    CallView call = processing.reprocess(callId);
    ZoneId zone =
        elderlyDirectory.find(call.elderlyId()).map(ElderlyView::zone).orElse(ZoneOffset.UTC);
    return CallResponse.of(call, zone);
  }
}
