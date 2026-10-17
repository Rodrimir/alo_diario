package br.com.cuida.analysis;

/** O modelo devolveu um resumo que não passa na validação. É repetido pelo {@link AiResilience}. */
class InvalidSummaryException extends RuntimeException {

  InvalidSummaryException(String message) {
    super("resumo inválido: " + message);
  }
}
