package br.com.cuida.agenda;

import br.com.cuida.agenda.model.AgendaRequestDTO;
import br.com.cuida.agenda.model.AgendaResponseDTO;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elderly/{elderlyId}/schedule")
class AgendaController {

  private final AgendaService agendaService;

  AgendaController(AgendaService agendaService) {
    this.agendaService = agendaService;
  }

  @GetMapping
  AgendaResponseDTO get(@PathVariable UUID elderlyId) {
    return agendaService.buscar(elderlyId);
  }

  @PutMapping
  AgendaResponseDTO update(
      @PathVariable UUID elderlyId, @Valid @RequestBody AgendaRequestDTO request) {
    return agendaService.atualizar(elderlyId, request);
  }
}
