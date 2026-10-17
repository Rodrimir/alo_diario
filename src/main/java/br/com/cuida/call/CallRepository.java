package br.com.cuida.call;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

interface CallRepository extends JpaRepository<Call, UUID> {

  Optional<Call> findByProviderCallId(String providerCallId);

  List<Call> findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscAttemptAsc(
      UUID elderlyId, LocalDate from, LocalDate to);

  List<Call> findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscCreatedAtAsc(
      UUID elderlyId, LocalDate from, LocalDate to);

  boolean existsByElderlyIdAndStatusIn(UUID elderlyId, Collection<CallStatus> statuses);

  long countByElderlyIdAndLocalDateAndTrigger(UUID elderlyId, LocalDate date, CallTrigger trigger);

  /** Já existe a primeira tentativa automática do dia? */
  @Query(
      """
      select count(c) > 0 from Call c
       where c.elderlyId = :elderlyId and c.localDate = :date and c.attempt = 1
         and c.trigger <> br.com.cuida.call.CallTrigger.MANUAL
      """)
  boolean existsFirstAutomaticAttempt(UUID elderlyId, LocalDate date);

  @Query(
      """
      select c.id from Call c
       where c.status = br.com.cuida.call.CallStatus.SCHEDULED and c.scheduledFor <= :now
       order by c.scheduledFor
      """)
  List<UUID> findIdsDueForDispatch(Instant now);

  /** Passa de SCHEDULED para DIALING só se ninguém pegou antes (disparo manual × job). */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Call c
         set c.status = br.com.cuida.call.CallStatus.DIALING,
             c.version = c.version + 1, c.updatedAt = :now
       where c.id = :id and c.status = br.com.cuida.call.CallStatus.SCHEDULED
      """)
  int claimForDialing(UUID id, Instant now);

  /** Grava o id do provedor sem disputar versão com o webhook, que pode chegar antes. */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Call c set c.provider = :provider, c.providerCallId = :providerCallId
       where c.id = :id and c.providerCallId is null
      """)
  int recordProvider(UUID id, String provider, String providerCallId);

  /** Tentativas automáticas sem sucesso e ainda sem a tentativa seguinte. */
  @Query(
      """
      select c from Call c
       where c.trigger <> br.com.cuida.call.CallTrigger.MANUAL
         and c.localDate >= :since
         and c.status in (br.com.cuida.call.CallStatus.NO_ANSWER,
                          br.com.cuida.call.CallStatus.BUSY,
                          br.com.cuida.call.CallStatus.FAILED)
         and not exists (select 1 from Call n
                          where n.elderlyId = c.elderlyId and n.localDate = c.localDate
                            and n.attempt = c.attempt + 1
                            and n.trigger <> br.com.cuida.call.CallTrigger.MANUAL)
      """)
  List<Call> findRetryCandidates(LocalDate since);

  @Query(
      """
      select c.id from Call c
       where c.status in (br.com.cuida.call.CallStatus.DIALING,
                          br.com.cuida.call.CallStatus.IN_PROGRESS)
         and c.updatedAt < :before
      """)
  List<UUID> findIdsActiveSince(Instant before);
}
