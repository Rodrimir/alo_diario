package br.com.cuida.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Clock} único da aplicação, em UTC. Todo código que depende do "agora" (agendador,
 * auditoria, retenção) recebe este bean, para que os testes possam controlar o tempo.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
