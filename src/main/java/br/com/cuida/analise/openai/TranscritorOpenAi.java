package br.com.cuida.analise.openai;

import br.com.cuida.analise.Transcritor;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

/**
 * Transcrição com a OpenAI. A API aceita até 25 MB por arquivo; com mp3 e o limite de 10 minutos
 * por ligação ficamos bem abaixo disso.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
class TranscritorOpenAi implements Transcritor {

  private final OpenAiAudioTranscriptionModel model;

  TranscritorOpenAi(OpenAiAudioTranscriptionModel model) {
    this.model = model;
  }

  @Override
  public String transcribe(byte[] audio, String filename) {
    ByteArrayResource resource =
        new ByteArrayResource(audio) {
          @Override
          public String getFilename() {
            return filename;
          }
        };
    return model.call(new AudioTranscriptionPrompt(resource)).getResult().getOutput();
  }
}
