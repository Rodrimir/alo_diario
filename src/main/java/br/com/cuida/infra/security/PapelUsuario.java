package br.com.cuida.infra.security;

/**
 * Papel do usuário, gravado como texto em {@code users.role} e enviado no claim {@code role} do
 * JWT. A authority do Spring Security é {@code "ROLE_" + name()}.
 */
public enum PapelUsuario {
  TUTOR,
  ADMIN;

  public String authority() {
    return "ROLE_" + name();
  }
}
