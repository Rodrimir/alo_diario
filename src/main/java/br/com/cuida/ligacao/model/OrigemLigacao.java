package br.com.cuida.ligacao.model;

/** Origem da ligação. Apenas {@code MANUAL} fica fora da regra de "uma ligação por dia". */
public enum OrigemLigacao {
  SCHEDULED,
  RETRY,
  MANUAL
}
