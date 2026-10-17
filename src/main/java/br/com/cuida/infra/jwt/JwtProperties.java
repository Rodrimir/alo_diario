package br.com.cuida.infra.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração do JWT (HS256).
 *
 * @param secret segredo com pelo menos 32 bytes ({@code JWT_SECRET})
 * @param ttl validade do token ({@code JWT_TTL}, padrão 1h)
 * @param issuer valor do claim {@code iss}
 * @param allowRandomSecret se {@code true} (só no perfil dev), gera um segredo aleatório quando
 *     {@code secret} está vazio
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
    String secret,
    @DefaultValue("PT1H") Duration ttl,
    @DefaultValue("cuida") String issuer,
    @DefaultValue("false") boolean allowRandomSecret) {}
