package br.com.cuida.infra.exception;

/** A operação viola uma regra de negócio (ex.: ligar sem consentimento). Responde 422. */
public class RegraDeNegocioException extends RuntimeException {

  public RegraDeNegocioException(String message) {
    super(message);
  }
}
