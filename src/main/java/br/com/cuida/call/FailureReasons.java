package br.com.cuida.call;

import java.util.Set;

/**
 * Códigos gravados em {@code calls.failure_reason} (às vezes seguidos de {@code ": detalhe"}). Só
 * {@link #DIAL_ERROR} e {@link #PROVIDER_ERROR} entram nas retentativas, junto com {@code
 * NO_ANSWER}/{@code BUSY}.
 */
public final class FailureReasons {

  /** Sem consentimento de gravação: o agendador não liga. */
  public static final String RECORDING_CONSENT_MISSING = "RECORDING_CONSENT_MISSING";

  /** Idoso desativado entre o agendamento e o disparo. */
  public static final String ELDERLY_INACTIVE = "ELDERLY_INACTIVE";

  /** O gateway de voz recusou ou não respondeu ao pedido de ligação. */
  public static final String DIAL_ERROR = "DIAL_ERROR";

  /** O provedor encerrou a ligação com erro (rede, telefonia, pipeline). */
  public static final String PROVIDER_ERROR = "PROVIDER_ERROR";

  /** Ligação cancelada manualmente no provedor. */
  public static final String CANCELED = "CANCELED";

  /** O horário passou da janela de recuperação (aplicação fora do ar). */
  public static final String MISSED_WINDOW = "MISSED_WINDOW";

  /** O relatório final do provedor não chegou dentro do prazo. */
  public static final String NO_PROVIDER_REPORT = "NO_PROVIDER_REPORT";

  static final Set<String> RETRYABLE = Set.of(DIAL_ERROR, PROVIDER_ERROR);

  private FailureReasons() {}

  static String withDetail(String code, String detail) {
    return detail == null || detail.isBlank() ? code : code + ": " + detail;
  }

  static boolean isRetryable(String failureReason) {
    if (failureReason == null) {
      return false;
    }
    int colon = failureReason.indexOf(':');
    String code = colon < 0 ? failureReason : failureReason.substring(0, colon);
    return RETRYABLE.contains(code);
  }
}
