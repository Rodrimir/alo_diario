package br.com.cuida.infra.saude;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check simples para monitoramento externo, sem autenticação: rotacionar credenciais não
 * pode derrubar o monitoramento. O health completo continua em {@code /actuator/health}.
 */
@RestController
@RequestMapping("/api")
class HealthController {

  @GetMapping("/ping")
  Map<String, String> ping() {
    return Map.of("status", "ok");
  }
}
