package br.com.cuida.infra.util;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Clock} único da aplicação, em UTC. Todo código que depende do "agora" (agendador,
 * auditoria, retenção) recebe este bean, para que os testes possam controlar o tempo.
 */
@Configuration(proxyBeanMethods = false)
class RelogioConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
