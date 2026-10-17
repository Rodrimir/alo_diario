package br.com.cuida.infra.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Origens do frontend autorizadas a chamar a API pelo navegador ({@code APP_CORS_ALLOWED_ORIGINS},
 * separadas por vírgula; aceita padrões como {@code http://localhost:*}). Vazio = nenhuma.
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(@DefaultValue List<String> allowedOrigins) {}
