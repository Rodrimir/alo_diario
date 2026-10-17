package br.com.cuida.ligacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cuida.ligacao.model.ClarezaResposta;
import br.com.cuida.ligacao.model.Humor;
import br.com.cuida.ligacao.model.Ligacao;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.ligacao.model.RespostaAtributo;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusLigacao;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import br.com.cuida.questionario.model.TipoResposta;
import br.com.cuida.testsupport.IntegrationTest;
import br.com.cuida.testsupport.TestFixtures;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class LigacaoPersistenciaIT {

  private static final QuestionarioSnapshot SNAPSHOT =
      new QuestionarioSnapshot(
          List.of(
              new QuestionarioSnapshot.Item("sleep", "Dormiu bem?", TipoResposta.YES_NO, 0),
              new QuestionarioSnapshot.Item(
                  "mood", "Como está o humor?", TipoResposta.FREE_TEXT, 1)));

  @Autowired LigacaoRepository ligacaoRepository;

  @Autowired JdbcTemplate jdbc;

  @Test
  void roundTripsSnapshotAndStructuredSummaryAsJsonb() {
    UUID elderlyId = newElderly();
    Ligacao call =
        new Ligacao(
            elderlyId,
            OrigemLigacao.SCHEDULED,
            Instant.parse("2026-10-16T12:00:00Z"),
            LocalDate.of(2026, 10, 16),
            1,
            SNAPSHOT);
    ResumoLigacao summary =
        new ResumoLigacao(
            "Dona Maria dormiu bem e está animada.",
            Humor.GOOD,
            List.of(
                new RespostaAtributo(
                    "sleep", "Dormiu bem?", "Sim, dormiu bem.", ClarezaResposta.CLEAR),
                new RespostaAtributo("mood", "Como está o humor?", "", ClarezaResposta.NOT_ASKED)),
            List.of("Vai receber a visita da neta."),
            NivelAlerta.NONE,
            List.of(),
            false);
    call.recordSummary(summary);
    ligacaoRepository.save(call);

    Ligacao loaded = ligacaoRepository.findById(call.getId()).orElseThrow();
    String mood =
        jdbc.queryForObject(
            "select structured_summary ->> 'mood' from calls where id = ?",
            String.class,
            call.getId());

    assertThat(loaded.getQuestionnaireSnapshot()).isEqualTo(SNAPSHOT);
    assertThat(loaded.getStructuredSummary()).isEqualTo(summary);
    assertThat(loaded.getSummaryText()).isEqualTo(summary.overview());
    assertThat(loaded.getAlertLevel()).isEqualTo(NivelAlerta.NONE);
    assertThat(loaded.getStatus()).isEqualTo(StatusLigacao.SCHEDULED);
    assertThat(loaded.getVersion()).isZero();
    assertThat(mood).isEqualTo("GOOD");
  }

  @Test
  void neverStoresTwoAutomaticAttemptsWithSameNumberOnSameDay() {
    UUID elderlyId = newElderly();
    LocalDate day = LocalDate.of(2026, 10, 16);
    Instant at = Instant.parse("2026-10-16T12:00:00Z");
    ligacaoRepository.saveAndFlush(
        new Ligacao(elderlyId, OrigemLigacao.SCHEDULED, at, day, 1, SNAPSHOT));

    assertThatThrownBy(
            () ->
                ligacaoRepository.saveAndFlush(
                    new Ligacao(elderlyId, OrigemLigacao.SCHEDULED, at, day, 1, SNAPSHOT)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void manualCallsAreOutsideTheDailyUniqueness() {
    UUID elderlyId = newElderly();
    LocalDate day = LocalDate.of(2026, 10, 16);
    Instant at = Instant.parse("2026-10-16T12:00:00Z");
    ligacaoRepository.saveAndFlush(
        new Ligacao(elderlyId, OrigemLigacao.SCHEDULED, at, day, 1, SNAPSHOT));
    ligacaoRepository.saveAndFlush(
        new Ligacao(elderlyId, OrigemLigacao.MANUAL, at, day, 1, SNAPSHOT));
    ligacaoRepository.saveAndFlush(
        new Ligacao(elderlyId, OrigemLigacao.MANUAL, at, day, 1, SNAPSHOT));

    assertThat(
            ligacaoRepository.findByElderlyIdAndLocalDateBetweenOrderByLocalDateAscAttemptAsc(
                elderlyId, day, day))
        .hasSize(3);
  }

  private UUID newElderly() {
    return TestFixtures.insertElderly(jdbc, TestFixtures.insertTutor(jdbc));
  }
}
