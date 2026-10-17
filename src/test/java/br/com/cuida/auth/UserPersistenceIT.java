package br.com.cuida.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.testsupport.IntegrationTest;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@IntegrationTest
class UserPersistenceIT {

  @Autowired UserRepository users;

  @Test
  void persistsUserWithRolesAndNormalizedEmail() {
    String email = "Tutor." + UUID.randomUUID() + "@Example.COM";
    User saved =
        users.save(new User("Ana", email, "hash", Set.of(Role.ROLE_TUTOR, Role.ROLE_ADMIN)));

    User loaded = users.findByEmail(User.normalizeEmail(email)).orElseThrow();

    assertThat(loaded.getId()).isEqualTo(saved.getId());
    assertThat(loaded.getEmail()).isEqualTo(email.toLowerCase());
    assertThat(loaded.getRoles()).containsExactlyInAnyOrder(Role.ROLE_TUTOR, Role.ROLE_ADMIN);
    assertThat(loaded.getCreatedAt()).isNotNull();
    assertThat(loaded.getUpdatedAt()).isNotNull();
  }

  @Test
  void rejectsDuplicateEmail() {
    String email = "dup-" + UUID.randomUUID() + "@example.com";
    users.save(new User("Ana", email, "hash", Set.of(Role.ROLE_TUTOR)));

    assertThatThrownBy(
            () -> users.saveAndFlush(new User("Bia", email, "hash", Set.of(Role.ROLE_TUTOR))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
