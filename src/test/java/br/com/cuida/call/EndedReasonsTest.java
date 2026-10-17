package br.com.cuida.call;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EndedReasonsTest {

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
  void mapsVapiEndedReason(String endedReason, CallStatus expected) {
    assertThat(EndedReasons.outcomeOf(endedReason).status()).isEqualTo(expected);
  }

  @Test
  void providerErrorsAreRetryableButCancelIsNot() {
    String providerError =
        EndedReasons.outcomeOf("pipeline-error-openai-llm-failed").failureReason();
    String canceled = EndedReasons.outcomeOf("manually-canceled").failureReason();

    assertThat(providerError).startsWith("PROVIDER_ERROR: ");
    assertThat(FailureReasons.isRetryable(providerError)).isTrue();
    assertThat(FailureReasons.isRetryable(canceled)).isFalse();
    assertThat(FailureReasons.isRetryable(FailureReasons.NO_PROVIDER_REPORT)).isFalse();
  }
}
