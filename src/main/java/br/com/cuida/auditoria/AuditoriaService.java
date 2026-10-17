package br.com.cuida.auditoria;

import br.com.cuida.auditoria.model.AcaoAuditada;
import br.com.cuida.auditoria.model.RegistroAcesso;
import br.com.cuida.auditoria.model.RegistroAcessoDTO;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Registra e consulta quem acessou resumo, transcrição ou áudio de um idoso (API pública). */
@Service
public class AuditoriaService {

  private final RegistroAcessoRepository registroAcessoRepository;
  private final Clock clock;

  AuditoriaService(RegistroAcessoRepository registroAcessoRepository, Clock clock) {
    this.registroAcessoRepository = registroAcessoRepository;
    this.clock = clock;
  }

  /** Grava em transação própria: o registro fica mesmo se a leitura falhar depois. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(UUID userId, UUID elderlyId, UUID callId, AcaoAuditada action, String ip) {
    registroAcessoRepository.save(
        new RegistroAcesso(userId, elderlyId, callId, action, ip, clock.instant()));
  }

  /** Os 500 acessos mais recentes aos dados do idoso. */
  @Transactional(readOnly = true)
  public List<RegistroAcessoDTO> findByElderly(UUID elderlyId) {
    return registroAcessoRepository.findTop500ByElderlyIdOrderByOccurredAtDesc(elderlyId).stream()
        .map(
            log ->
                new RegistroAcessoDTO(
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
