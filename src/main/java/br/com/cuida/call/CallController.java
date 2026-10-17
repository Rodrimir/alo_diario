package br.com.cuida.call;

import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CallController {

  private final ManualCallService manualCalls;

  CallController(ManualCallService manualCalls) {
    this.manualCalls = manualCalls;
  }

  /** Dispara uma ligação agora ("ligar agora"). */
  @PostMapping("/api/elderly/{elderlyId}/calls")
  CallResponse callNow(@PathVariable UUID elderlyId) {
    return manualCalls.callNow(elderlyId);
  }
}
