package br.com.cuida.infra.exception;

/** O recurso não existe ou não pertence ao usuário (nunca revela qual dos dois). Responde 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

  public RecursoNaoEncontradoException(String message) {
    super(message);
  }
}
