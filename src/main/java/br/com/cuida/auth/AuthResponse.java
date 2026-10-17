package br.com.cuida.auth;

import br.com.cuida.shared.security.UserRole;
import java.util.UUID;

/**
 * Resposta de login e registro.
 *
 * @param token JWT para o header {@code Authorization: Bearer <token>}
 * @param expiresIn validade do token em segundos
 */
record AuthResponse(
    String token, long expiresIn, UUID userId, String name, String email, UserRole role) {}
