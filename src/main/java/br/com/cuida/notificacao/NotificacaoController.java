package br.com.cuida.notificacao;

import br.com.cuida.notificacao.model.NotificacaoResponseDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calls/{callId}/notifications")
class NotificacaoController {

  private final NotificacaoService notificacaoService;

  NotificacaoController(NotificacaoService notificacaoService) {
    this.notificacaoService = notificacaoService;
  }

  /** Avisos enviados (ou não) à família sobre esta ligação. */
  @GetMapping
  List<NotificacaoResponseDTO> list(@PathVariable UUID callId) {
    return notificacaoService.listar(callId);
  }

  /** Envia de novo o resumo ao WhatsApp do responsável. */
  @PostMapping("/resend")
  NotificacaoResponseDTO resend(@PathVariable UUID callId) {
    return notificacaoService.reenviar(callId);
  }
}
