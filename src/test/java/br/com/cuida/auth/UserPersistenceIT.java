package br.com.cuida.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.shared.security.UserRole;
import br.com.cuida.testsupport.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class UserPersistenceIT {

  @Autowired UserRepository users;

  @Autowired JdbcTemplate jdbc;

  @Test
  void persistsUserWithRoleAndNormalizedEmail() {
    String email = "Tutor." + UUID.randomUUID() + "@Example.COM";
    User saved = users.save(new User("Ana", email, "hash", UserRole.ADMIN));

    User loaded = users.findByEmail(User.normalizeEmail(email)).orElseThrow();
    String rawRole =
        jdbc.queryForObject("select role from users where id = ?", String.class, saved.getId());

    assertThat(loaded.getId()).isEqualTo(saved.getId());
    assertThat(loaded.getEmail()).isEqualTo(email.toLowerCase());
    assertThat(loaded.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(rawRole).isEqualTo("ADMIN");
    assertThat(loaded.getCreatedAt()).isNotNull();
    assertThat(loaded.getUpdatedAt()).isNotNull();
  }

  @Test
  void rejectsDuplicateEmail() {
    String email = "dup-" + UUID.randomUUID() + "@example.com";
    users.save(new User("Ana", email, "hash", UserRole.TUTOR));

    assertThatThrownBy(() -> users.saveAndFlush(new User("Bia", email, "hash", UserRole.TUTOR)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
