package br.com.cuida.infra.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TelefoneNormalizadoTest {

  @ParameterizedTest
  @CsvSource({
    "'(11) 98765-4321', +5511987654321",
    "'11 3456-7890', +551134567890",
    "5511987654321, +5511987654321",
    "'+55 21 99876-5432', +5521998765432",
    "'+1 (415) 555-0100', +14155550100"
  })
  void normalizesToE164(String raw, String expected) {
    assertThat(TelefoneNormalizado.toE164(raw)).contains(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "123", "abc", "+0 11 1234-5678", "+1234567890123456"})
  void rejectsInvalidNumbers(String raw) {
    assertThat(TelefoneNormalizado.toE164(raw)).isEmpty();
  }
}
