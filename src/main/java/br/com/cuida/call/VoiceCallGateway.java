package br.com.cuida.call;

/**
 * Porta para o gateway de voz que liga, conversa com a pessoa idosa, grava e avisa o fim por
 * webhook. Adaptadores: {@code call.vapi} (produção) e {@code call.mock} (dev/test).
 */
public interface VoiceCallGateway {

  /** Nome gravado em {@code calls.provider} e usado nos webhooks. */
  String provider();

  /**
   * Pede ao provedor que faça a ligação. Não é repetido automaticamente: um pedido que estourou o
   * tempo pode já ter criado a ligação, e repetir faria a pessoa receber duas.
   *
   * @throws VoiceCallException se o provedor recusar ou não responder
   */
  StartedCall start(OutboundCall call);
}
