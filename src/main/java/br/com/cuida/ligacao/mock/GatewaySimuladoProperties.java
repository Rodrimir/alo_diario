package br.com.cuida.ligacao.mock;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Gateway de voz simulado.
 *
 * @param delay tempo entre o disparo e a entrega dos webhooks simulados
 * @param recordingUrl gravação de exemplo "devolvida" pelo provedor (áudio sintético)
 */
@ConfigurationProperties("app.voice.mock")
record GatewaySimuladoProperties(
    @DefaultValue("PT3S") Duration delay,
    @DefaultValue("classpath:mock/sample-call.mp3") String recordingUrl) {}
