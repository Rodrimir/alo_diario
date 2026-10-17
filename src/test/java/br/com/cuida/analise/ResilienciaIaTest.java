package br.com.cuida.analise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ResilienciaIaTest {

  @Test
  void retriesTransientFailuresUpToThreeTimes() {
    ResilienciaIa resilience = new ResilienciaIa(Duration.ofMillis(10));
    AtomicInteger calls = new AtomicInteger();

    String result =
        resilience.call(
            () -> {
              if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("instável");
              }
              return "ok";
            });

    assertThat(result).isEqualTo("ok");
    assertThat(calls).hasValue(3);
  }

  @Test
  void givesUpAfterThreeAttempts() {
    ResilienciaIa resilience = new ResilienciaIa(Duration.ofMillis(10));
    AtomicInteger calls = new AtomicInteger();

    assertThatThrownBy(
            () ->
                resilience.call(
                    () -> {
                      calls.incrementAndGet();
                      throw new IllegalStateException("fora do ar");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(calls).hasValue(3);
  }
}
