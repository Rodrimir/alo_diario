package br.com.cuida.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita os listeners assíncronos ({@code @ApplicationModuleListener}). O executor é o {@code
 * applicationTaskExecutor} do Spring Boot, limitado em {@code spring.task.execution.pool.*}, para o
 * pipeline pós-ligação não esgotar conexões nem a cota das APIs externas.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfig {}
