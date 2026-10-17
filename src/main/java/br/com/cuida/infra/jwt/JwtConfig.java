package br.com.cuida.infra.jwt;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Chave HS256, encoder e decoder do JWT emitido pela própria aplicação. */
@Configuration(proxyBeanMethods = false)
class JwtConfig {

  private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
  private static final int MIN_SECRET_BYTES = 32;

  @Bean
  SecretKey jwtSecretKey(JwtProperties properties) {
    String secret = properties.secret();
    if (secret == null || secret.isBlank()) {
      if (!properties.allowRandomSecret()) {
        throw new IllegalStateException(
            "JWT_SECRET é obrigatório (mínimo de " + MIN_SECRET_BYTES + " bytes)");
      }
      byte[] random = new byte[48];
      new SecureRandom().nextBytes(random);
      secret = Base64.getEncoder().encodeToString(random);
      log.warn("JWT_SECRET vazio: usando um segredo aleatório (tokens caem a cada restart)");
    }
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET precisa ter pelo menos " + MIN_SECRET_BYTES + " bytes");
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(properties.issuer()), semAudienciaMcp()));
    return decoder;
  }

  /** Token de ligação (audiência mcp) nunca vale como login de usuário em {@code /api}. */
  private static OAuth2TokenValidator<Jwt> semAudienciaMcp() {
    return jwt ->
        jwt.getAudience() != null && jwt.getAudience().contains(ClaimsJwt.AUDIENCE_MCP)
            ? OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Token de ligação não vale para a API", null))
            : OAuth2TokenValidatorResult.success();
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    return NimbusJwtEncoder.withSecretKey(jwtSecretKey).algorithm(MacAlgorithm.HS256).build();
  }
}
