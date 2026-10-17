package br.com.cuida.ligacao;

/** O gateway de voz recusou ou não respondeu ao pedido de ligação. */
public class LigacaoRecusadaException extends RuntimeException {

  public LigacaoRecusadaException(String message) {
    super(message);
  }

  public LigacaoRecusadaException(String message, Throwable cause) {
    super(message, cause);
  }
}
