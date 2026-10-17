package br.com.cuida.infra.exception;

/** O usuário está autenticado, mas o papel dele não permite a operação. Responde 403. */
public class AcessoNegadoException extends RuntimeException {

  public AcessoNegadoException(String message) {
    super(message);
  }
}
