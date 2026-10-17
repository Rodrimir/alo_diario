package br.com.cuida.analise;

import com.openai.errors.OpenAIServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Retry + circuit breaker para as chamadas de IA. Nenhum erro 4xx é repetido (inclusive 429:
 * repetir só piora a cota); falhas de rede, 5xx e resposta inválida do modelo são repetidas até 3
 * vezes com espera exponencial. Os retries internos do SDK ficam desligados para não multiplicar
 * tentativas.
 */
@Component
public class ResilienciaIa {

  private final Retry retry;
  private final CircuitBreaker circuitBreaker;

  ResilienciaIa() {
    this(Duration.ofSeconds(2));
  }

  /**
   * @param initialWait espera antes da 2ª tentativa (dobra a cada tentativa)
   */
  ResilienciaIa(Duration initialWait) {
    this.retry =
        Retry.of(
            "ai",
            RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialWait, 2))
                .retryOnException(ResilienciaIa::isRetryable)
                .build());
    this.circuitBreaker =
        CircuitBreaker.of(
            "ai",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
  }

  public <T> T call(Supplier<T> supplier) {
    return Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, supplier))
        .get();
  }

  static boolean isRetryable(Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (current instanceof OpenAIServiceException service) {
        int status = service.statusCode();
        return status < 400 || status >= 500;
      }
      if (current instanceof CallNotPermittedException) {
        return false;
      }
    }
    return true;
  }
}
