package br.com.cuida.infra.security;

import java.util.Objects;
import java.util.UUID;

/** Usuário autenticado na requisição atual, extraído do JWT. */
public record UsuarioLogado(UUID id, PapelUsuario role) {

  public UsuarioLogado {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(role, "role");
  }

  public boolean isAdmin() {
    return role == PapelUsuario.ADMIN;
  }

  /** Administrador acessa tudo; tutor só o que é dele. */
  public boolean canAccess(UUID ownerId) {
    return isAdmin() || id.equals(ownerId);
  }
}
