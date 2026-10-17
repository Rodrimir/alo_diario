package br.com.cuida.analise;

/** Porta de transcrição de áudio (pt-BR). */
public interface Transcritor {

  /**
   * @param audio conteúdo do áudio
   * @param filename nome com extensão (o provedor usa para reconhecer o formato)
   */
  String transcribe(byte[] audio, String filename);
}
