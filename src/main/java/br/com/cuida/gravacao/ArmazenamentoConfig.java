package br.com.cuida.gravacao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Escolhe o adaptador de armazenamento por {@code app.storage.provider}. */
@Configuration(proxyBeanMethods = false)
class ArmazenamentoConfig {

  @Bean
  ArmazenamentoDeAudio audioStorage(ArmazenamentoProperties properties) {
    return switch (properties.provider()) {
      case "s3" -> new ArmazenamentoS3(properties.s3());
      case "filesystem" -> new ArmazenamentoEmDisco(properties.filesystem().directory());
      default ->
          throw new IllegalStateException(
              "app.storage.provider inválido: "
                  + properties.provider()
                  + " (use s3 ou filesystem)");
    };
  }
}
