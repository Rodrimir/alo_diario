package br.com.cuida.shared.security;

import java.util.Objects;
import java.util.UUID;

/** Usuário autenticado na requisição atual, extraído do JWT. */
public record CurrentUser(UUID id, UserRole role) {

  public CurrentUser {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(role, "role");
  }

  public boolean isAdmin() {
    return role == UserRole.ADMIN;
  }

  /** Administrador acessa tudo; tutor só o que é dele. */
  public boolean canAccess(UUID ownerId) {
    return isAdmin() || id.equals(ownerId);
  }
}
