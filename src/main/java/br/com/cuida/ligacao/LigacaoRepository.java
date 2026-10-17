package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.ligacao.model.StatusLigacao;
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

interface LigacaoRepository extends JpaRepository<Ligacao, UUID> {

  Optional<Ligacao> findByProviderCallId(String providerCallId);

  List<Ligacao> findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscAttemptAsc(
      UUID elderlyId, LocalDate from, LocalDate to);

  List<Ligacao> findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscCreatedAtAsc(
      UUID elderlyId, LocalDate from, LocalDate to);

  boolean existsByElderlyIdAndStatusIn(UUID elderlyId, Collection<StatusLigacao> statuses);

  long countByElderlyIdAndLocalDateAndTrigger(
      UUID elderlyId, LocalDate date, OrigemLigacao trigger);

  /** Já existe a primeira tentativa automática do dia? */
  @Query(
      """
      select count(c) > 0 from Ligacao c
       where c.elderlyId = :elderlyId and c.localDate = :date and c.attempt = 1
         and c.trigger <> br.com.cuida.ligacao.model.OrigemLigacao.MANUAL
      """)
  boolean existsFirstAutomaticAttempt(UUID elderlyId, LocalDate date);

  @Query(
      """
      select c.id from Ligacao c
       where c.status = br.com.cuida.ligacao.model.StatusLigacao.SCHEDULED and c.scheduledFor <= :now
       order by c.scheduledFor
      """)
  List<UUID> findIdsDueForDispatch(Instant now);

  /** Passa de SCHEDULED para DIALING só se ninguém pegou antes (disparo manual × job). */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Ligacao c
         set c.status = br.com.cuida.ligacao.model.StatusLigacao.DIALING,
             c.version = c.version + 1, c.updatedAt = :now
       where c.id = :id and c.status = br.com.cuida.ligacao.model.StatusLigacao.SCHEDULED
      """)
  int claimForDialing(UUID id, Instant now);

  /** Grava o id do provedor sem disputar versão com o webhook, que pode chegar antes. */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Ligacao c set c.provider = :provider, c.providerCallId = :providerCallId
       where c.id = :id and c.providerCallId is null
      """)
  int recordProvider(UUID id, String provider, String providerCallId);

  /** Tentativas automáticas sem sucesso, ainda não avaliadas pelo planejador de retentativas. */
  @Query(
      """
      select c from Ligacao c
       where c.trigger <> br.com.cuida.ligacao.model.OrigemLigacao.MANUAL
         and c.localDate >= :since
         and c.retryClosed = false
         and c.status in (br.com.cuida.ligacao.model.StatusLigacao.NO_ANSWER,
                          br.com.cuida.ligacao.model.StatusLigacao.BUSY,
                          br.com.cuida.ligacao.model.StatusLigacao.FAILED)
         and not exists (select 1 from Ligacao n
                          where n.elderlyId = c.elderlyId and n.localDate = c.localDate
                            and n.attempt = c.attempt + 1
                            and n.trigger <> br.com.cuida.ligacao.model.OrigemLigacao.MANUAL)
      """)
  List<Ligacao> findRetryCandidates(LocalDate since);

  @Query(
      """
      select c.id from Ligacao c
       where c.audioObjectKey is not null and c.endedAt < :before
      """)
  List<UUID> findIdsWithAudioEndedBefore(Instant before);

  @Query(
      """
      select c.id from Ligacao c
       where c.status in (br.com.cuida.ligacao.model.StatusLigacao.DIALING,
                          br.com.cuida.ligacao.model.StatusLigacao.IN_PROGRESS)
         and c.updatedAt < :before
      """)
  List<UUID> findIdsActiveSince(Instant before);
}
