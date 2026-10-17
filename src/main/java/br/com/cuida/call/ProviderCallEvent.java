package br.com.cuida.call;

import java.time.Instant;
import java.util.UUID;

/** Evento do provedor de voz, já traduzido do formato dele (webhook). */
public sealed interface ProviderCallEvent {

  /**
   * Como achar a ligação: pelo nosso id (metadado) ou pelo id do provedor.
   *
   * @param callId nosso id, se o provedor devolveu o metadado
   * @param providerCallId id da ligação no provedor
   */
  record CallRef(UUID callId, String providerCallId) {}

  CallRef ref();

  /** Mudança de estado (ex.: {@code in-progress}). */
  record StatusUpdate(CallRef ref, String status, Instant at) implements ProviderCallEvent {}

  /** Relatório final da ligação. */
  record Report(
      CallRef ref,
      String endedReason,
      Instant startedAt,
      Instant endedAt,
      Integer durationSeconds,
      String transcript,
      String recordingUrl)
      implements ProviderCallEvent {}
}
