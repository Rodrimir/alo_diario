package br.com.cuida.shared.persistence;

import java.time.Clock;
import java.util.Optional;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuração de persistência compartilhada.
 *
 * <ul>
 *   <li>Auditoria ({@code createdAt}/{@code updatedAt}) baseada no {@link Clock} injetado.
 *   <li>Colunas {@code jsonb} ({@code @JdbcTypeCode(SqlTypes.JSON)}) serializadas pelo mesmo {@link
 *       JsonMapper} (Jackson 3) usado pela API, via o {@code Jackson3JsonFormatMapper} nativo do
 *       Hibernate 7.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class PersistenceConfig {

  @Bean
  DateTimeProvider auditingDateTimeProvider(Clock clock) {
    return () -> Optional.of(clock.instant());
  }

  @Bean
  HibernatePropertiesCustomizer jsonFormatMapperCustomizer(JsonMapper jsonMapper) {
    return properties ->
        properties.put(
            MappingSettings.JSON_FORMAT_MAPPER, new Jackson3JsonFormatMapper(jsonMapper));
  }
}
