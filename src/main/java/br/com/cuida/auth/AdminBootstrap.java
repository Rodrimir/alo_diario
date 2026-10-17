package br.com.cuida.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * O registro público só cria tutores. O administrador vem da configuração, para não existir um
 * endpoint de "virar admin".
 */
@Component
class AdminBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

  private final BootstrapAdminProperties properties;
  private final AuthService authService;

  AdminBootstrap(BootstrapAdminProperties properties, AuthService authService) {
    this.properties = properties;
    this.authService = authService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.configured()) {
      return;
    }
    if (authService.createAdminIfAbsent(
        properties.name(), properties.email(), properties.password())) {
      log.info("Administrador inicial criado");
    }
  }
}
