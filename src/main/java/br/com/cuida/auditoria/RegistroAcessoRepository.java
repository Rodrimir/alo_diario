package br.com.cuida.auditoria;

import br.com.cuida.auditoria.model.RegistroAcesso;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegistroAcessoRepository extends JpaRepository<RegistroAcesso, UUID> {

  List<RegistroAcesso> findByCallIdOrderByOccurredAtAsc(UUID callId);

  List<RegistroAcesso> findTop500ByElderlyIdOrderByOccurredAtDesc(UUID elderlyId);
}
