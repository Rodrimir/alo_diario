package br.com.cuida.auth;

import br.com.cuida.shared.persistence.AuditableEntity;
import br.com.cuida.shared.security.UserRole;
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
class User extends AuditableEntity {

  @Column(nullable = false, length = 120)
  private String name;

  /** Sempre normalizado em minúsculas (garantido também por CHECK no banco). */
  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  /** Exigido pelo JPA. */
  protected User() {}

  User(String name, String email, String passwordHash, UserRole role) {
    super(UUID.randomUUID());
    this.name = Objects.requireNonNull(name, "name").trim();
    this.email = normalizeEmail(email);
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    this.role = Objects.requireNonNull(role, "role");
  }

  static String normalizeEmail(String email) {
    return Objects.requireNonNull(email, "email").trim().toLowerCase(Locale.ROOT);
  }

  String getName() {
    return name;
  }

  String getEmail() {
    return email;
  }

  String getPasswordHash() {
    return passwordHash;
  }

  UserRole getRole() {
    return role;
  }
}
