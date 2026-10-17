package br.com.cuida.gravacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DownloadDeGravacaoTest {

  @Test
  void rejectsUrlsThatCouldBeSsrf() {
    DownloadDeGravacao fetcher = fetcher(false);

    assertThatThrownBy(() -> fetcher.fetch("http://storage.vapi.ai/a.mp3"))
        .hasMessageContaining("https");
    assertThatThrownBy(() -> fetcher.fetch("https://169.254.169.254/latest/meta-data"))
        .hasMessageContaining("não permitido");
    assertThatThrownBy(() -> fetcher.fetch("https://vapi.ai.evil.com/a.mp3"))
        .hasMessageContaining("não permitido");
    assertThatThrownBy(() -> fetcher.fetch("file:///etc/passwd"))
        .isInstanceOf(DownloadDeGravacao.RecordingRejectedException.class);
    assertThatThrownBy(() -> fetcher.fetch("classpath:mock/sample-call.mp3"))
        .hasMessageContaining("classpath");
  }

  @Test
  void readsTheMockRecordingWhenClasspathIsAllowed() {
    DownloadDeGravacao.Download download = fetcher(true).fetch("classpath:mock/sample-call.mp3");

    assertThat(download.contentType()).isEqualTo("audio/mpeg");
    assertThat(download.content()).hasSizeGreaterThan(1000);
  }

  private static DownloadDeGravacao fetcher(boolean allowClasspath) {
    return new DownloadDeGravacao(
        new ArmazenamentoProperties(
            "filesystem",
            new ArmazenamentoProperties.S3(null, "us-east-1", "b", null, null, true),
            new ArmazenamentoProperties.Filesystem(Path.of("target")),
            new ArmazenamentoProperties.Download(
                List.of("storage.vapi.ai", "*.vapi.ai"),
                allowClasspath,
                1_000_000,
                Duration.ofSeconds(5))));
  }
}
