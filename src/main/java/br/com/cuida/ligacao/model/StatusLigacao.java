package br.com.cuida.ligacao.model;

/** Estado de uma ligação ao longo do ciclo agenda → liga → conversa → processa. */
public enum StatusLigacao {
  SCHEDULED,
  DIALING,
  IN_PROGRESS,
  COMPLETED,
  NO_ANSWER,
  BUSY,
  FAILED,
  PROCESSING,
  PROCESSED,
  PROCESSING_FAILED
}
