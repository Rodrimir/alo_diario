package br.com.cuida.shared.persistence;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Persistable;

/**
 * Base das entidades com id {@link UUID} atribuído pela aplicação.
 *
 * <p>Implementa {@link Persistable} para que o Spring Data faça {@code persist} (e não {@code
 * merge} com um SELECT extra) em entidades novas, mesmo com o id já preenchido.
 */
@MappedSuperclass
public abstract class BaseEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Transient private boolean persisted;

  /** Exigido pelo JPA. */
  protected BaseEntity() {}

  protected BaseEntity(UUID id) {
    this.id = Objects.requireNonNull(id, "id");
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return !persisted;
  }

  @PostLoad
  @PostPersist
  void markPersisted() {
    this.persisted = true;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BaseEntity that)) {
      return false;
    }
    return Hibernate.getClass(this) == Hibernate.getClass(that) && id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Hibernate.getClass(this).hashCode();
  }
}
