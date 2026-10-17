package br.com.cuida.auth;

import br.com.cuida.shared.persistence.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 30)
  private Set<Role> roles = new HashSet<>();

  /** Exigido pelo JPA. */
  protected User() {}

  User(String name, String email, String passwordHash, Set<Role> roles) {
    super(UUID.randomUUID());
    this.name = Objects.requireNonNull(name, "name");
    this.email = normalizeEmail(email);
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    this.roles = new HashSet<>(roles);
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

  Set<Role> getRoles() {
    return Collections.unmodifiableSet(roles);
  }
}
