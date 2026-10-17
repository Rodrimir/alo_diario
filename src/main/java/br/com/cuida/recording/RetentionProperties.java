package br.com.cuida.recording;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Prazo de guarda dos áudios ({@code APP_RETENTION_AUDIO_DAYS}, padrão 90 dias). */
@ConfigurationProperties("app.retention")
record RetentionProperties(@DefaultValue("90") int audioDays) {}
