package br.com.cuida.call;

/** De onde veio a transcrição: do gateway de voz ou da nossa transcrição (Whisper/OpenAI). */
public enum TranscriptSource {
  PROVIDER,
  WHISPER
}
