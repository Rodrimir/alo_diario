package br.com.cuida.gravacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.gravacao.model.ConteudoAudio;
import java.io.InputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArmazenamentoEmDiscoTest {

  @TempDir Path root;

  @Test
  void storesReadsAndDeletesByPrefix() throws Exception {
    ArmazenamentoEmDisco storage = new ArmazenamentoEmDisco(root);
    storage.put("elderly/a/calls/1.mp3", new byte[] {1, 2, 3}, "audio/mpeg");
    storage.put("elderly/a/calls/2.wav", new byte[] {4}, "audio/wav");
    storage.put("elderly/b/calls/3.mp3", new byte[] {5}, "audio/mpeg");

    ConteudoAudio content = storage.get("elderly/a/calls/1.mp3").orElseThrow();
    try (InputStream in = content.stream()) {
      assertThat(in.readAllBytes()).containsExactly(1, 2, 3);
    }
    assertThat(content.contentType()).isEqualTo("audio/mpeg");
    assertThat(storage.get("elderly/a/calls/2.wav").orElseThrow().contentType())
        .isEqualTo("audio/wav");

    assertThat(storage.deletePrefix("elderly/a/")).isEqualTo(2);
    assertThat(storage.get("elderly/a/calls/1.mp3")).isEmpty();
    assertThat(storage.get("elderly/b/calls/3.mp3")).isPresent();
  }

  @Test
  void rejectsKeysOutsideTheRoot() {
    ArmazenamentoEmDisco storage = new ArmazenamentoEmDisco(root);

    assertThatThrownBy(() -> storage.put("../fora.mp3", new byte[] {1}, "audio/mpeg"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
