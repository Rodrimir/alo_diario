package br.com.cuida.infra.agendamento;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Jobs agendados ({@code @Scheduled}) com lock distribuído (ShedLock, tabela {@code shedlock}): com
 * várias instâncias, cada job roda em uma só. {@code app.jobs.enabled=false} desliga todos (perfil
 * de testes, que chama os jobs diretamente).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = "app.jobs.enabled", matchIfMissing = true)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class AgendamentoConfig {

  @Bean
  LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .usingDbTime()
            .build());
  }
}
