package br.com.cuida.ligacao.model;

/** De onde veio a transcrição: do gateway de voz ou da nossa transcrição (Whisper/OpenAI). */
public enum OrigemTranscricao {
  PROVIDER,
  WHISPER
}
