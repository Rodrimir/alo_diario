package br.com.cuida.ligacao.model;

/**
 * Nível de alerta de uma ligação. {@code URGENT} apenas quando a pessoa relata algo como queda, dor
 * forte, falta de ar, confusão ou pedido de ajuda.
 */
public enum NivelAlerta {
  NONE,
  ATTENTION,
  URGENT
}
