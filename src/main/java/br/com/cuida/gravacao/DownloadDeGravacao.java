package br.com.cuida.gravacao;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Baixa a gravação do provedor. Só aceita HTTPS de hosts da lista permitida (a URL vem do webhook e
 * não pode virar SSRF), com limite de tamanho e tempo. {@code classpath:} só com o gateway mock.
 * Erros de rede e 5xx são repetidos (3 tentativas); 4xx não.
 */
@Component
class DownloadDeGravacao {

  /** Gravação baixada. */
  record Download(byte[] content, String contentType) {}

  private final ArmazenamentoProperties.Download properties;
  private final HttpClient http;
  private final Retry retry;

  DownloadDeGravacao(ArmazenamentoProperties properties) {
    this.properties = properties.download();
    this.http =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    this.retry =
        Retry.of(
            "recording-download",
            RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryOnException(e -> !(e instanceof RecordingRejectedException))
                .build());
  }

  Download fetch(String url) {
    if (url.startsWith("classpath:")) {
      return fromClasspath(url);
    }
    URI uri = URI.create(url);
    requireAllowed(uri);
    return retry.executeSupplier(() -> download(uri, url));
  }

  private Download download(URI uri, String url) {
    HttpRequest request = HttpRequest.newBuilder(uri).timeout(properties.timeout()).GET().build();
    try {
      HttpResponse<InputStream> response =
          http.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream body = response.body()) {
        int status = response.statusCode();
        if (status >= 400 && status < 500) {
          throw new RecordingRejectedException("download da gravação recusado: HTTP " + status);
        }
        if (status != 200) {
          throw new IOException("download da gravação falhou: HTTP " + status);
        }
        byte[] content =
            body.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, properties.maxBytes() + 1));
        if (content.length > properties.maxBytes()) {
          throw new RecordingRejectedException("gravação maior que o limite permitido");
        }
        String contentType =
            FormatosDeAudio.normalize(
                response.headers().firstValue("Content-Type").orElse(null), url);
        return new Download(content, contentType);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("download interrompido", e);
    }
  }

  private Download fromClasspath(String url) {
    if (!properties.allowClasspath()) {
      throw new RecordingRejectedException("URL classpath: não permitida");
    }
    String path = url.substring("classpath:".length());
    try (InputStream in = new ClassPathResource(path).getInputStream()) {
      return new Download(in.readAllBytes(), FormatosDeAudio.contentTypeOf(path));
    } catch (IOException e) {
      throw new RecordingRejectedException("gravação de exemplo não encontrada: " + path);
    }
  }

  private void requireAllowed(URI uri) {
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
      throw new RecordingRejectedException("a gravação precisa ser uma URL https");
    }
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    boolean allowed =
        properties.allowedHosts().stream()
            .map(h -> h.toLowerCase(Locale.ROOT))
            .anyMatch(
                pattern ->
                    pattern.startsWith("*.")
                        ? host.endsWith(pattern.substring(1))
                        : host.equals(pattern));
    if (!allowed) {
      throw new RecordingRejectedException("host da gravação não permitido: " + host);
    }
  }

  /** A gravação não pode ser baixada (não adianta repetir). */
  static class RecordingRejectedException extends RuntimeException {
    RecordingRejectedException(String message) {
      super(message);
    }
  }
}
