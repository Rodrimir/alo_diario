package br.com.cuida.analise;

/**
 * O modelo devolveu um resumo que não passa na validação. É repetido pelo {@link ResilienciaIa}.
 */
class ResumoInvalidoException extends RuntimeException {

  ResumoInvalidoException(String message) {
    super("resumo inválido: " + message);
  }
}
