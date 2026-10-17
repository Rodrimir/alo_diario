package br.com.cuida.notificacao;

import br.com.cuida.notificacao.model.Notificacao;
import br.com.cuida.notificacao.model.StatusNotificacao;
import br.com.cuida.notificacao.model.TipoNotificacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

  boolean existsByDedupKey(String dedupKey);

  List<Notificacao> findByCallIdOrderByCreatedAtAsc(UUID callId);

  Optional<Notificacao> findFirstByCallIdAndKindOrderByCreatedAtDesc(
      UUID callId, TipoNotificacao kind);

  List<Notificacao> findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
      StatusNotificacao status, int attempts);
}
