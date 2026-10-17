package br.com.cuida.shared.security;

/** Nomes dos claims próprios do token. {@code sub} é o id do usuário. */
final class JwtClaims {

  static final String ROLE = "role";

  private JwtClaims() {}
}
