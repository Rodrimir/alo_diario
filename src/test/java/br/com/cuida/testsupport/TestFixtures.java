package br.com.cuida.testsupport;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Inserções mínimas via SQL para satisfazer chaves estrangeiras entre módulos sem acessar
 * entidades/repositórios de outra feature (que são package-private).
 */
public final class TestFixtures {

  private TestFixtures() {}

  public static UUID insertTutor(JdbcTemplate jdbc) {
    UUID id = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "insert into users (id, name, email, password_hash, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?)",
        id,
        "Tutor de Teste",
        "tutor-" + id + "@teste.local",
        "{bcrypt}placeholder",
        now,
        now);
    jdbc.update("insert into user_roles (user_id, role) values (?, 'ROLE_TUTOR')", id);
    return id;
  }

  public static UUID insertElderly(JdbcTemplate jdbc, UUID tutorId) {
    UUID id = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "insert into elderly (id, tutor_id, name, phone_number, timezone, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?, ?)",
        id,
        tutorId,
        "Maria da Silva",
        "+5511987654321",
        "America/Sao_Paulo",
        now,
        now);
    return id;
  }
}
