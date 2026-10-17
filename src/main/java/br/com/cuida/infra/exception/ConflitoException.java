package br.com.cuida.infra.exception;

/** Conflito com o estado atual (ex.: e-mail já cadastrado, ligação em andamento). Responde 409. */
public class ConflitoException extends RuntimeException {

  public ConflitoException(String message) {
    super(message);
  }
}
