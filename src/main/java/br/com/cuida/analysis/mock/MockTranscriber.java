package br.com.cuida.analysis.mock;

import br.com.cuida.analysis.Transcriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Transcrição simulada (sem chave de API). Só é usada no modo {@code WHISPER}. */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
class MockTranscriber implements Transcriber {

  @Override
  public String transcribe(byte[] audio, String filename) {
    return """
        Assistente: Olá! Aqui é a Cuida. Esta ligação é gravada. Podemos conversar um pouquinho?
        Idoso(a): Pode sim.
        Assistente: Como a pessoa dormiu esta noite?
        Idoso(a): Dormi bem.
        Assistente: Obrigada pela conversa! Até amanhã.
        Idoso(a): Até amanhã.""";
  }
}
