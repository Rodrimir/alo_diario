package br.com.cuida.recording;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Escolhe o adaptador de armazenamento por {@code app.storage.provider}. */
@Configuration(proxyBeanMethods = false)
class StorageConfig {

  @Bean
  AudioStorage audioStorage(StorageProperties properties) {
    return switch (properties.provider()) {
      case "s3" -> new S3AudioStorage(properties.s3());
      case "filesystem" -> new FileSystemAudioStorage(properties.filesystem().directory());
      default ->
          throw new IllegalStateException(
              "app.storage.provider inválido: "
                  + properties.provider()
                  + " (use s3 ou filesystem)");
    };
  }
}
