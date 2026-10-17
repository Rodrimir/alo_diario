package br.com.cuida;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.testsupport.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
@AutoConfigureMockMvc
class ApplicationIT {

  @Autowired MockMvcTester mvc;

  @Autowired JdbcTemplate jdbc;

  @Test
  void healthIsUp() {
    assertThat(mvc.get().uri("/actuator/health"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("UP");
  }

  @Test
  void flywayAppliedBaselineAndHibernateValidatedSchema() {
    Integer applied =
        jdbc.queryForObject(
            "select count(*) from flyway_schema_history where version = '1' and success",
            Integer.class);
    assertThat(applied).isEqualTo(1);
  }

  @Test
  void infrastructureTablesExist() {
    assertThat(tableExists("shedlock")).isTrue();
    assertThat(tableExists("event_publication")).isTrue();
  }

  private boolean tableExists(String name) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists (select 1 from information_schema.tables where table_name = ?)",
            Boolean.class,
            name));
  }
}
