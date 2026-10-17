package br.com.cuida.notificacao;

/** O whats-api recusou ou não respondeu ao envio. */
public class EnvioWhatsAppException extends RuntimeException {

  public EnvioWhatsAppException(String message) {
    super(message);
  }

  public EnvioWhatsAppException(String message, Throwable cause) {
    super(message, cause);
  }
}
