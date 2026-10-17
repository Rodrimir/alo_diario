package br.com.cuida.ligacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.idoso.model.IdosoDTO;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class RoteiroDaConversaTest {

  private static final LigacaoProperties PROPERTIES =
      new LigacaoProperties(Duration.ofHours(2), Duration.ofMinutes(30), 600, "Alô Diário");

  @ParameterizedTest
  @CsvSource({
    "05:00, Bom dia",
    "11:59, Bom dia",
    "12:00, Boa tarde",
    "17:59, Boa tarde",
    "18:00, Boa noite",
    "02:00, Boa noite"
  })
  void greetingFollowsTheLocalHour(LocalTime time, String expected) {
    assertThat(RoteiroDaConversa.cumprimento(time)).isEqualTo(expected);
  }

  @Test
  void openingUsesTheElderlyTimezoneAndAlwaysWarnsAboutRecording() {
    // 13:30 UTC = 10:30 em São Paulo (bom dia), mas 09:30 em Nova York também (bom dia) e 22:30 em
    // Tóquio (boa noite).
    Clock clock = Clock.fixed(Instant.parse("2026-10-17T13:30:00Z"), ZoneOffset.UTC);
    RoteiroDaConversa roteiro =
        new RoteiroDaConversa(
            PROPERTIES,
            clock,
            new StaticListableBeanFactory().getBeanProvider(ProvedorDeContexto.class));

    String saoPaulo = roteiro.firstMessage(elderly("America/Sao_Paulo"));
    String tokyo = roteiro.firstMessage(elderly("Asia/Tokyo"));

    assertThat(saoPaulo)
        .isEqualTo(
            "Bom dia, Dona Maria! Aqui é a assistente virtual do Alô Diário. Esta ligação é"
                + " gravada para que sua família acompanhe como você está. Gostaria de saber"
                + " como você está hoje.");
    assertThat(tokyo).startsWith("Boa noite, Dona Maria!");
  }

  private static IdosoDTO elderly(String zone) {
    return new IdosoDTO(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Maria da Silva",
        "Dona Maria",
        "+5511987654321",
        ZoneId.of(zone),
        true,
        true,
        null,
        null);
  }
}
