package br.com.cuida.autenticacao;

import br.com.cuida.autenticacao.model.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

  /** Espera o e-mail já normalizado ({@link Usuario#normalizeEmail(String)}). */
  Optional<Usuario> findByEmail(String email);

  boolean existsByEmail(String email);
}
