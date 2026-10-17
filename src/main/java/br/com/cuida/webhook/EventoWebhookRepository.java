package br.com.cuida.webhook;

import br.com.cuida.webhook.model.EventoWebhook;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface EventoWebhookRepository extends JpaRepository<EventoWebhook, UUID> {

  Optional<EventoWebhook> findByProviderAndEventId(String provider, String eventId);

  boolean existsByProviderAndEventId(String provider, String eventId);

  @Query(
      """
      select w from EventoWebhook w
       where w.status = br.com.cuida.webhook.model.StatusWebhook.FAILED
         and w.attempts < :maxAttempts
       order by w.receivedAt
      """)
  List<EventoWebhook> findRetryable(int maxAttempts);

  @Query(
      """
      select w from EventoWebhook w
       where w.status in (br.com.cuida.webhook.model.StatusWebhook.PENDING,
                          br.com.cuida.webhook.model.StatusWebhook.FAILED)
         and w.payload is not null
         and w.receivedAt < :before
      """)
  List<EventoWebhook> findUnfinishedWithPayloadReceivedBefore(Instant before);
}
