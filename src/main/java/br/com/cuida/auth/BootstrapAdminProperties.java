package br.com.cuida.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Administrador criado na inicialização ({@code APP_BOOTSTRAP_ADMIN_EMAIL}/{@code _PASSWORD}).
 * Vazio = nenhum administrador é criado.
 */
@ConfigurationProperties("app.bootstrap.admin")
record BootstrapAdminProperties(
    String email, String password, @DefaultValue("Administrador") String name) {

  boolean configured() {
    return email != null && !email.isBlank() && password != null && !password.isBlank();
  }
}
