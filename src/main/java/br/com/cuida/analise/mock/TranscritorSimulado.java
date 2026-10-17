package br.com.cuida.analise.mock;

import br.com.cuida.analise.Transcritor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Transcrição simulada (sem chave de API). Só é usada no modo {@code WHISPER}. */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
class TranscritorSimulado implements Transcritor {

  @Override
  public String transcribe(byte[] audio, String filename) {
    return """
        Assistente: Bom dia! Aqui é a assistente virtual do Alô Diário. Esta ligação é gravada. Podemos conversar um pouquinho?
        Idoso(a): Pode sim.
        Assistente: Como dormiu esta noite?
        Idoso(a): Dormi bem.
        Assistente: Obrigada pela conversa! Até amanhã.
        Idoso(a): Até amanhã.""";
  }
}
