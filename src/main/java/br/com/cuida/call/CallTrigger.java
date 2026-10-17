package br.com.cuida.call;

/** Origem da ligação. Apenas {@code MANUAL} fica fora da regra de "uma ligação por dia". */
public enum CallTrigger {
  SCHEDULED,
  RETRY,
  MANUAL
}
