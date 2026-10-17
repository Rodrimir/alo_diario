package br.com.cuida.call;

/** O gateway de voz recusou ou não respondeu ao pedido de ligação. */
public class VoiceCallException extends RuntimeException {

  public VoiceCallException(String message) {
    super(message);
  }

  public VoiceCallException(String message, Throwable cause) {
    super(message, cause);
  }
}
