package br.com.cuida.autenticacao;

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
class CargaDoAdministradorInicial implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CargaDoAdministradorInicial.class);

  private final AdministradorInicialProperties administradorInicialProperties;
  private final AuthService authService;

  CargaDoAdministradorInicial(
      AdministradorInicialProperties administradorInicialProperties, AuthService authService) {
    this.administradorInicialProperties = administradorInicialProperties;
    this.authService = authService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!administradorInicialProperties.configured()) {
      return;
    }
    if (authService.createAdminIfAbsent(
        administradorInicialProperties.name(),
        administradorInicialProperties.email(),
        administradorInicialProperties.password())) {
      log.info("Administrador inicial criado");
    }
  }
}
