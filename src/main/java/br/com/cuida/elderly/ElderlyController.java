package br.com.cuida.elderly;

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
class ElderlyController {

  private final ElderlyService service;

  ElderlyController(ElderlyService service) {
    this.service = service;
  }

  @PostMapping
  ElderlyResponse create(@Valid @RequestBody ElderlyRequest request) {
    return service.create(request);
  }

  @GetMapping
  List<ElderlyResponse> list() {
    return service.list();
  }

  @GetMapping("/{elderlyId}")
  ElderlyResponse get(@PathVariable UUID elderlyId) {
    return service.get(elderlyId);
  }

  @PutMapping("/{elderlyId}")
  ElderlyResponse update(@PathVariable UUID elderlyId, @Valid @RequestBody ElderlyRequest request) {
    return service.update(elderlyId, request);
  }

  @PutMapping("/{elderlyId}/consent")
  ElderlyResponse consent(
      @PathVariable UUID elderlyId, @Valid @RequestBody ConsentRequest request) {
    return service.recordConsent(elderlyId, request.recordingConsent());
  }

  /** Exclusão definitiva (LGPD) do idoso e de tudo o que é dele. */
  @DeleteMapping("/{elderlyId}")
  ResponseEntity<Void> delete(@PathVariable UUID elderlyId) {
    service.delete(elderlyId);
    return ResponseEntity.ok().build();
  }
}
