package br.com.cuida.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<User, UUID> {

  /** Espera o e-mail já normalizado ({@link User#normalizeEmail(String)}). */
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
