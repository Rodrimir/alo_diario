package br.com.cuida.notificacao.model;

/** O que está sendo avisado à família. */
public enum TipoNotificacao {
  /** Resumo da ligação do dia (✅/⚠️ + recado). */
  CALL_SUMMARY,
  /** Nenhuma tentativa automática do dia conseguiu falar com a pessoa. */
  NO_CONTACT,
  /** Alerta urgente sinalizado durante a ligação. */
  URGENT_ALERT
}
