package br.com.cuida.autenticacao.model;

import br.com.cuida.infra.security.PapelUsuario;
import java.util.UUID;

/**
 * Resposta de login e registro.
 *
 * @param token JWT para o header {@code Authorization: Bearer <token>}
 * @param expiresIn validade do token em segundos
 */
public record AuthResponseDTO(
    String token, long expiresIn, UUID userId, String name, String email, PapelUsuario role) {}
