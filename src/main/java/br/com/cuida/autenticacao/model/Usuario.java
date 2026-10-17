package br.com.cuida.autenticacao.model;

import br.com.cuida.infra.persistencia.EntidadeAuditavel;
import br.com.cuida.infra.security.PapelUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Usuário autenticável (tutor ou administrador). */
@Entity
@Table(name = "users")
public class Usuario extends EntidadeAuditavel {

  @Column(nullable = false, length = 120)
  private String name;

  /** Sempre normalizado em minúsculas (garantido também por CHECK no banco). */
  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PapelUsuario role;

  /** Exigido pelo JPA. */
  protected Usuario() {}

  public Usuario(String name, String email, String passwordHash, PapelUsuario role) {
    super(UUID.randomUUID());
    this.name = Objects.requireNonNull(name, "name").trim();
    this.email = normalizeEmail(email);
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    this.role = Objects.requireNonNull(role, "role");
  }

  public static String normalizeEmail(String email) {
    return Objects.requireNonNull(email, "email").trim().toLowerCase(Locale.ROOT);
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public PapelUsuario getRole() {
    return role;
  }
}
