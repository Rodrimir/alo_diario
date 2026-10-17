package br.com.cuida.analysis;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração da IA.
 *
 * @param provider {@code mock} (padrão, sem chave) ou {@code openai}
 * @param transcriptionMode {@code PROVIDER} usa a transcrição do gateway de voz; {@code WHISPER}
 *     sempre transcreve o áudio com o nosso provedor
 */
@ConfigurationProperties("app.ai")
public record AiProperties(
    @DefaultValue("mock") String provider,
    @DefaultValue("PROVIDER") TranscriptionMode transcriptionMode,
    @DefaultValue OpenAi openai) {

  /** De onde vem a transcrição. */
  public enum TranscriptionMode {
    PROVIDER,
    WHISPER
  }

  /**
   * @param apiKey {@code OPENAI_API_KEY}
   * @param chatModel modelo do resumo; vazio = padrão do Spring AI
   * @param transcriptionModel modelo de transcrição
   * @param timeout tempo máximo de cada chamada
   */
  public record OpenAi(
      String apiKey,
      String baseUrl,
      String chatModel,
      @DefaultValue("gpt-4o-mini-transcribe") String transcriptionModel,
      @DefaultValue("PT90S") Duration timeout) {}
}
