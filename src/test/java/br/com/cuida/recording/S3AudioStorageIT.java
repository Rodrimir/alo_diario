package br.com.cuida.recording;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Adaptador S3 contra um MinIO real (mesma imagem do docker-compose). */
@Testcontainers
class S3AudioStorageIT {

  static final String USER = "teste";
  static final String PASSWORD = "senha-de-teste-123";
  static final String BUCKET = "gravacoes-teste";

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>(DockerImageName.parse("cgr.dev/chainguard/minio:latest"))
          .withCommand("server", "/data")
          .withEnv("MINIO_ROOT_USER", USER)
          .withEnv("MINIO_ROOT_PASSWORD", PASSWORD)
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @Test
  void putGetAndDeletePrefix() throws Exception {
    String endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
    try (S3Client admin =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(USER, PASSWORD)))
            .build()) {
      admin.createBucket(request -> request.bucket(BUCKET));
    }
    try (S3AudioStorage storage =
        new S3AudioStorage(
            new StorageProperties.S3(endpoint, "us-east-1", BUCKET, USER, PASSWORD, true))) {
      storage.put("elderly/x/calls/1.mp3", new byte[] {7, 8, 9}, "audio/mpeg");
      storage.put("elderly/x/calls/2.mp3", new byte[] {1}, "audio/mpeg");

      AudioContent content = storage.get("elderly/x/calls/1.mp3").orElseThrow();
      try (InputStream in = content.stream()) {
        assertThat(in.readAllBytes()).containsExactly(7, 8, 9);
      }
      assertThat(content.contentType()).isEqualTo("audio/mpeg");
      assertThat(content.contentLength()).isEqualTo(3);
      assertThat(storage.get("nao/existe.mp3")).isEmpty();

      assertThat(storage.deletePrefix("elderly/x/")).isEqualTo(2);
      assertThat(storage.get("elderly/x/calls/1.mp3")).isEmpty();
    }
  }
}
