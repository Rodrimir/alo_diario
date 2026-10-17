package br.com.cuida.webhook.model;

/** Situação de um webhook recebido. */
public enum StatusWebhook {
  /** Gravado, aguardando (ou em) processamento. */
  PENDING,
  /** Processado com sucesso; o payload já foi apagado. */
  PROCESSED,
  /** O processamento falhou; será tentado de novo até o limite de tentativas. */
  FAILED,
  /** Recebido mas sem efeito (ex.: ligação desconhecida); o payload já foi apagado. */
  IGNORED
}
