package br.com.cuida.idoso;

import br.com.cuida.idoso.model.ConsentimentoRequestDTO;
import br.com.cuida.idoso.model.IdosoRequestDTO;
import br.com.cuida.idoso.model.IdosoResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elderly")
class IdosoController {

  private final IdosoService idosoService;

  IdosoController(IdosoService idosoService) {
    this.idosoService = idosoService;
  }

  @PostMapping
  IdosoResponseDTO create(@Valid @RequestBody IdosoRequestDTO request) {
    return idosoService.create(request);
  }

  @GetMapping
  List<IdosoResponseDTO> list() {
    return idosoService.list();
  }

  @GetMapping("/{elderlyId}")
  IdosoResponseDTO get(@PathVariable UUID elderlyId) {
    return idosoService.get(elderlyId);
  }

  @PutMapping("/{elderlyId}")
  IdosoResponseDTO update(
      @PathVariable UUID elderlyId, @Valid @RequestBody IdosoRequestDTO request) {
    return idosoService.update(elderlyId, request);
  }

  @PutMapping("/{elderlyId}/consent")
  IdosoResponseDTO consent(
      @PathVariable UUID elderlyId, @Valid @RequestBody ConsentimentoRequestDTO request) {
    return idosoService.recordConsent(elderlyId, request.recordingConsent());
  }

  /** Exclusão definitiva (LGPD) do idoso e de tudo o que é dele. */
  @DeleteMapping("/{elderlyId}")
  ResponseEntity<Void> delete(@PathVariable UUID elderlyId) {
    idosoService.delete(elderlyId);
    return ResponseEntity.ok().build();
  }
}
