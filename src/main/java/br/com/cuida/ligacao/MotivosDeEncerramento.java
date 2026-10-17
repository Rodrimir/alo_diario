package br.com.cuida.ligacao;

import br.com.cuida.ligacao.model.MotivosDeFalha;
import br.com.cuida.ligacao.model.StatusLigacao;
import java.util.Locale;
import java.util.Set;

/** Traduz o motivo de encerramento do provedor (Vapi {@code endedReason}) em status da ligação. */
final class MotivosDeEncerramento {

  private static final Set<String> NO_ANSWER =
      Set.of("customer-did-not-answer", "voicemail", "customer-did-not-give-microphone-permission");

  private static final Set<String> BUSY = Set.of("customer-busy");

  private static final Set<String> CANCELED = Set.of("manually-canceled");

  private MotivosDeEncerramento() {}

  /** Status e motivo de falha ({@code null} se não houve falha). */
  record Outcome(StatusLigacao status, String failureReason) {}

  static Outcome outcomeOf(String endedReason) {
    String reason = endedReason == null ? "" : endedReason.trim().toLowerCase(Locale.ROOT);
    if (NO_ANSWER.contains(reason)) {
      return new Outcome(StatusLigacao.NO_ANSWER, null);
    }
    if (BUSY.contains(reason)) {
      return new Outcome(StatusLigacao.BUSY, null);
    }
    if (CANCELED.contains(reason)) {
      return new Outcome(StatusLigacao.FAILED, MotivosDeFalha.CANCELED);
    }
    if (reason.isEmpty()
        || reason.contains("error")
        || reason.contains("failed")
        || reason.contains("fault")
        || reason.startsWith("pipeline-")
        || reason.startsWith("call.start")) {
      return new Outcome(
          StatusLigacao.FAILED, MotivosDeFalha.withDetail(MotivosDeFalha.PROVIDER_ERROR, reason));
    }
    return new Outcome(StatusLigacao.COMPLETED, null);
  }
}
