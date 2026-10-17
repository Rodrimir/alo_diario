package br.com.cuida.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecordingFetcherTest {

  @Test
  void rejectsUrlsThatCouldBeSsrf() {
    RecordingFetcher fetcher = fetcher(false);

    assertThatThrownBy(() -> fetcher.fetch("http://storage.vapi.ai/a.mp3"))
        .hasMessageContaining("https");
    assertThatThrownBy(() -> fetcher.fetch("https://169.254.169.254/latest/meta-data"))
        .hasMessageContaining("não permitido");
    assertThatThrownBy(() -> fetcher.fetch("https://vapi.ai.evil.com/a.mp3"))
        .hasMessageContaining("não permitido");
    assertThatThrownBy(() -> fetcher.fetch("file:///etc/passwd"))
        .isInstanceOf(RecordingFetcher.RecordingRejectedException.class);
    assertThatThrownBy(() -> fetcher.fetch("classpath:mock/sample-call.mp3"))
        .hasMessageContaining("classpath");
  }

  @Test
  void readsTheMockRecordingWhenClasspathIsAllowed() {
    RecordingFetcher.Download download = fetcher(true).fetch("classpath:mock/sample-call.mp3");

    assertThat(download.contentType()).isEqualTo("audio/mpeg");
    assertThat(download.content()).hasSizeGreaterThan(1000);
  }

  private static RecordingFetcher fetcher(boolean allowClasspath) {
    return new RecordingFetcher(
        new StorageProperties(
            "filesystem",
            new StorageProperties.S3(null, "us-east-1", "b", null, null, true),
            new StorageProperties.Filesystem(Path.of("target")),
            new StorageProperties.Download(
                List.of("storage.vapi.ai", "*.vapi.ai"),
                allowClasspath,
                1_000_000,
                Duration.ofSeconds(5))));
  }
}
