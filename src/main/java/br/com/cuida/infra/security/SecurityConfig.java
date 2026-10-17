package br.com.cuida.infra.security;

import br.com.cuida.infra.jwt.CustomJwtAuthenticationConverter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Segurança stateless com JWT HS256 emitido pela própria aplicação.
 *
 * <p>Rotas públicas: login, registro, ping, webhooks (validados por segredo no próprio controller),
 * health e Swagger (este só existe quando habilitado).
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CustomJwtAuthenticationConverter jwtAuthenticationConverter,
      JsonMapper jsonMapper)
      throws Exception {
    ErroDeSegurancaHandler errors = new ErroDeSegurancaHandler(jsonMapper);
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/login", "/api/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/ping")
                    .permitAll()
                    .requestMatchers("/webhook/**")
                    .permitAll()
                    .requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasRole(PapelUsuario.ADMIN.name())
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(errors)
                    .accessDeniedHandler(errors))
        .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors));
    return http.build();
  }

  /**
   * CORS para o frontend em outro domínio. O token vai no header {@code Authorization} (sem
   * cookie), então não há credenciais de navegador; o áudio expõe os headers que o player precisa.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOriginPatterns(
        properties.allowedOrigins().stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Range"));
    cors.setExposedHeaders(
        List.of("Content-Disposition", "Content-Length", "Content-Range", "Accept-Ranges"));
    cors.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
