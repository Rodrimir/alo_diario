package br.com.cuida.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.autenticacao.model.Usuario;
import br.com.cuida.infra.security.PapelUsuario;
import br.com.cuida.testsupport.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UsuarioPersistenciaIT {

  @Autowired UsuarioRepository usuarioRepository;

  @Autowired JdbcTemplate jdbc;

  @Test
  void persistsUserWithRoleAndNormalizedEmail() {
    String email = "Tutor." + UUID.randomUUID() + "@Example.COM";
    Usuario saved = usuarioRepository.save(new Usuario("Ana", email, "hash", PapelUsuario.ADMIN));

    Usuario loaded = usuarioRepository.findByEmail(Usuario.normalizeEmail(email)).orElseThrow();
    String rawRole =
        jdbc.queryForObject("select role from users where id = ?", String.class, saved.getId());

    assertThat(loaded.getId()).isEqualTo(saved.getId());
    assertThat(loaded.getEmail()).isEqualTo(email.toLowerCase());
    assertThat(loaded.getRole()).isEqualTo(PapelUsuario.ADMIN);
    assertThat(rawRole).isEqualTo("ADMIN");
    assertThat(loaded.getCreatedAt()).isNotNull();
    assertThat(loaded.getUpdatedAt()).isNotNull();
  }

  @Test
  void rejectsDuplicateEmail() {
    String email = "dup-" + UUID.randomUUID() + "@example.com";
    usuarioRepository.save(new Usuario("Ana", email, "hash", PapelUsuario.TUTOR));

    assertThatThrownBy(
            () ->
                usuarioRepository.saveAndFlush(
                    new Usuario("Bia", email, "hash", PapelUsuario.TUTOR)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
