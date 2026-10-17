package br.com.cuida.call;

import java.util.Locale;
import java.util.Set;

/** Traduz o motivo de encerramento do provedor (Vapi {@code endedReason}) em status da ligação. */
final class EndedReasons {

  private static final Set<String> NO_ANSWER =
      Set.of("customer-did-not-answer", "voicemail", "customer-did-not-give-microphone-permission");

  private static final Set<String> BUSY = Set.of("customer-busy");

  private static final Set<String> CANCELED = Set.of("manually-canceled");

  private EndedReasons() {}

  /** Status e motivo de falha ({@code null} se não houve falha). */
  record Outcome(CallStatus status, String failureReason) {}

  static Outcome outcomeOf(String endedReason) {
    String reason = endedReason == null ? "" : endedReason.trim().toLowerCase(Locale.ROOT);
    if (NO_ANSWER.contains(reason)) {
      return new Outcome(CallStatus.NO_ANSWER, null);
    }
    if (BUSY.contains(reason)) {
      return new Outcome(CallStatus.BUSY, null);
    }
    if (CANCELED.contains(reason)) {
      return new Outcome(CallStatus.FAILED, FailureReasons.CANCELED);
    }
    if (reason.isEmpty()
        || reason.contains("error")
        || reason.contains("failed")
        || reason.contains("fault")
        || reason.startsWith("pipeline-")
        || reason.startsWith("call.start")) {
      return new Outcome(
          CallStatus.FAILED, FailureReasons.withDetail(FailureReasons.PROVIDER_ERROR, reason));
    }
    return new Outcome(CallStatus.COMPLETED, null);
  }
}
