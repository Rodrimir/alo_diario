package br.com.cuida.infra.exception;

/** O recurso existiu, mas foi apagado (ex.: áudio expurgado pela retenção). Responde 410. */
public class RecursoRemovidoException extends RuntimeException {

  public RecursoRemovidoException(String message) {
    super(message);
  }
}
