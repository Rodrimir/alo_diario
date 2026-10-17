package br.com.cuida.shared.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados do OpenAPI e esquema Bearer. O Swagger UI só fica ativo com {@code
 * SWAGGER_ENABLED=true} (ligado por padrão no perfil dev, desligado nos demais).
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

  private static final String BEARER = "bearerAuth";

  @Bean
  OpenAPI cuidaOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Cuida API")
                .version("v1")
                .description(
                    "Monitoramento conversacional diário de pessoas idosas. Faça login em"
                        + " POST /api/login e use o token em Authorize."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER));
  }
}
