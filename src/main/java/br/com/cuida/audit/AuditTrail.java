package br.com.cuida.audit;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Registra e consulta quem acessou resumo, transcrição ou áudio de um idoso (API pública). */
@Service
public class AuditTrail {

  private final AccessAuditLogRepository repository;
  private final Clock clock;

  AuditTrail(AccessAuditLogRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Grava em transação própria: o registro fica mesmo se a leitura falhar depois. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(UUID userId, UUID elderlyId, UUID callId, AuditAction action, String ip) {
    repository.save(new AccessAuditLog(userId, elderlyId, callId, action, ip, clock.instant()));
  }

  /** Os 500 acessos mais recentes aos dados do idoso. */
  @Transactional(readOnly = true)
  public List<AuditEntry> findByElderly(UUID elderlyId) {
    return repository.findTop500ByElderlyIdOrderByOccurredAtDesc(elderlyId).stream()
        .map(
            log ->
                new AuditEntry(
                    log.getId(),
                    log.getUserId(),
                    log.getElderlyId(),
                    log.getCallId(),
                    log.getAction(),
                    log.getIp(),
                    log.getOccurredAt()))
        .toList();
  }
}
