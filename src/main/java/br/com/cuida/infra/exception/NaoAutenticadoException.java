package br.com.cuida.infra.exception;

/** Credenciais inválidas ou usuário não autenticado. Responde 401. */
public class NaoAutenticadoException extends RuntimeException {

  public NaoAutenticadoException(String message) {
    super(message);
  }
}
