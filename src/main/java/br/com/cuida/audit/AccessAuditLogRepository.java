package br.com.cuida.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AccessAuditLogRepository extends JpaRepository<AccessAuditLog, UUID> {

  List<AccessAuditLog> findByCallIdOrderByOccurredAtAsc(UUID callId);
}
