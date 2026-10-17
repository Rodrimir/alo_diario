package br.com.cuida.infra.jwt;

/**
 * Token emitido no login/registro.
 *
 * @param token JWT assinado
 * @param expiresIn validade em segundos
 */
public record TokenEmitido(String token, long expiresIn) {}
