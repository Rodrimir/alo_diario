package br.com.cuida.notificacao.model;

/** Situação do envio. */
public enum StatusNotificacao {
  /** Registrado, ainda não enviado. */
  PENDING,
  /** Aceito pelo whats-api. */
  SENT,
  /** O envio falhou; o job tenta de novo até 3 vezes. */
  FAILED,
  /** Não enviado (ex.: idoso sem WhatsApp do responsável). */
  SKIPPED
}
