package br.com.cuida.gravacao;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Prazo de guarda dos áudios ({@code APP_RETENTION_AUDIO_DAYS}, padrão 90 dias). */
@ConfigurationProperties("app.retention")
record RetencaoProperties(@DefaultValue("90") int audioDays) {}
