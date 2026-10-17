package br.com.cuida.call;

/** Estado de uma ligação ao longo do ciclo agenda → liga → conversa → processa. */
public enum CallStatus {
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
