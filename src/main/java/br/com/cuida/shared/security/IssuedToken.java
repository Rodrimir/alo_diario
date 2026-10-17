package br.com.cuida.shared.security;

/**
 * Token emitido no login/registro.
 *
 * @param token JWT assinado
 * @param expiresIn validade em segundos
 */
public record IssuedToken(String token, long expiresIn) {}
