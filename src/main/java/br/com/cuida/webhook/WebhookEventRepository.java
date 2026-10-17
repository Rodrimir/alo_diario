package br.com.cuida.webhook;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

  Optional<WebhookEvent> findByProviderAndEventId(String provider, String eventId);

  boolean existsByProviderAndEventId(String provider, String eventId);

  @Query(
      """
      select w from WebhookEvent w
       where w.status = br.com.cuida.webhook.WebhookStatus.FAILED
         and w.attempts < :maxAttempts
       order by w.receivedAt
      """)
  List<WebhookEvent> findRetryable(int maxAttempts);

  @Query(
      """
      select w from WebhookEvent w
       where w.status in (br.com.cuida.webhook.WebhookStatus.PENDING,
                          br.com.cuida.webhook.WebhookStatus.FAILED)
         and w.payload is not null
         and w.receivedAt < :before
      """)
  List<WebhookEvent> findUnfinishedWithPayloadReceivedBefore(Instant before);
}
