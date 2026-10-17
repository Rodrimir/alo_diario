package br.com.cuida.analysis.openai;

import br.com.cuida.analysis.AiProperties;
import java.time.Duration;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Modelos da OpenAI (Spring AI), montados à mão e só com {@code app.ai.provider=openai}: sem chave,
 * nada da OpenAI é criado. Os retries do SDK ficam em 0 (quem repete é o {@code AiResilience}).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
class OpenAiConfig {

  @Bean
  OpenAiChatModel openAiChatModel(AiProperties properties) {
    AiProperties.OpenAi openai = requireKey(properties);
    OpenAiChatOptions.Builder options =
        OpenAiChatOptions.builder()
            .apiKey(openai.apiKey())
            .maxRetries(0)
            .timeout(openai.timeout())
            .temperature(0.1);
    if (openai.baseUrl() != null && !openai.baseUrl().isBlank()) {
      options.baseUrl(openai.baseUrl());
    }
    if (openai.chatModel() != null && !openai.chatModel().isBlank()) {
      options.model(openai.chatModel());
    }
    return OpenAiChatModel.builder().options(options.build()).build();
  }

  @Bean
  OpenAiAudioTranscriptionModel openAiTranscriptionModel(AiProperties properties) {
    AiProperties.OpenAi openai = requireKey(properties);
    OpenAiAudioTranscriptionOptions.Builder options =
        OpenAiAudioTranscriptionOptions.builder().language("pt").temperature(0f);
    options.apiKey(openai.apiKey());
    options.model(openai.transcriptionModel());
    options.maxRetries(0);
    options.timeout(openai.timeout().plus(Duration.ofSeconds(60)));
    if (openai.baseUrl() != null && !openai.baseUrl().isBlank()) {
      options.baseUrl(openai.baseUrl());
    }
    return OpenAiAudioTranscriptionModel.builder().options(options.build()).build();
  }

  private static AiProperties.OpenAi requireKey(AiProperties properties) {
    AiProperties.OpenAi openai = properties.openai();
    if (openai.apiKey() == null || openai.apiKey().isBlank()) {
      throw new IllegalStateException("app.ai.provider=openai exige OPENAI_API_KEY");
    }
    return openai;
  }
}
