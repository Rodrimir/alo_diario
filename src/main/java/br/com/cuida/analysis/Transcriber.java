package br.com.cuida.analysis;

/** Porta de transcrição de áudio (pt-BR). */
public interface Transcriber {

  /**
   * @param audio conteúdo do áudio
   * @param filename nome com extensão (o provedor usa para reconhecer o formato)
   */
  String transcribe(byte[] audio, String filename);
}
