package br.com.cuida.recording;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Armazenamento e cópia das gravações.
 *
 * @param provider {@code s3} (MinIO/AWS) ou {@code filesystem} (pasta local)
 * @param s3 conexão S3
 * @param filesystem pasta local
 * @param download regras para baixar a gravação do provedor
 */
@ConfigurationProperties("app.storage")
record StorageProperties(
    @DefaultValue("s3") String provider,
    @DefaultValue S3 s3,
    @DefaultValue Filesystem filesystem,
    @DefaultValue Download download) {

  /**
   * @param endpoint vazio = AWS; preenchido = S3 compatível (MinIO)
   * @param pathStyle obrigatório para o MinIO
   */
  record S3(
      String endpoint,
      @DefaultValue("us-east-1") String region,
      @DefaultValue("cuida-recordings") String bucket,
      String accessKey,
      String secretKey,
      @DefaultValue("true") boolean pathStyle) {}

  record Filesystem(@DefaultValue("./data/recordings") Path directory) {}

  /**
   * @param allowedHosts hosts de onde a gravação pode ser baixada (proteção contra SSRF); {@code
   *     *.dominio} vale para subdomínios
   * @param allowClasspath aceita {@code classpath:} (só para o gateway mock)
   * @param maxBytes tamanho máximo aceito
   * @param timeout tempo máximo do download
   */
  record Download(
      @DefaultValue({"storage.vapi.ai", "*.vapi.ai"}) List<String> allowedHosts,
      @DefaultValue("false") boolean allowClasspath,
      @DefaultValue("52428800") long maxBytes,
      @DefaultValue("PT60S") Duration timeout) {}
}
