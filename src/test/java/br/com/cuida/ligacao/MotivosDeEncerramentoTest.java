package br.com.cuida.ligacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cuida.ligacao.model.MotivosDeFalha;
import br.com.cuida.ligacao.model.StatusLigacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MotivosDeEncerramentoTest {

  @ParameterizedTest
  @CsvSource({
    "customer-ended-call, COMPLETED",
    "assistant-ended-call, COMPLETED",
    "silence-timed-out, COMPLETED",
    "exceeded-max-duration, COMPLETED",
    "customer-did-not-answer, NO_ANSWER",
    "voicemail, NO_ANSWER",
    "customer-busy, BUSY",
    "twilio-failed-to-connect-call, FAILED",
    "pipeline-error-openai-llm-failed, FAILED",
    "manually-canceled, FAILED"
  })
  void mapsVapiEndedReason(String endedReason, StatusLigacao expected) {
    assertThat(MotivosDeEncerramento.outcomeOf(endedReason).status()).isEqualTo(expected);
  }

  @Test
  void providerErrorsAreRetryableButCancelIsNot() {
    String providerError =
        MotivosDeEncerramento.outcomeOf("pipeline-error-openai-llm-failed").failureReason();
    String canceled = MotivosDeEncerramento.outcomeOf("manually-canceled").failureReason();

    assertThat(providerError).startsWith("PROVIDER_ERROR: ");
    assertThat(MotivosDeFalha.isRetryable(providerError)).isTrue();
    assertThat(MotivosDeFalha.isRetryable(canceled)).isFalse();
    assertThat(MotivosDeFalha.isRetryable(MotivosDeFalha.NO_PROVIDER_REPORT)).isFalse();
  }
}
