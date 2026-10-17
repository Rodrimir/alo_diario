package br.com.cuida.call;

/**
 * Nível de alerta de uma ligação. {@code URGENT} apenas quando a pessoa relata algo como queda, dor
 * forte, falta de ar, confusão ou pedido de ajuda.
 */
public enum AlertLevel {
  NONE,
  ATTENTION,
  URGENT
}
