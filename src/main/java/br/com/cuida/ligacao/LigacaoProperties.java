package br.com.cuida.ligacao;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Regras do ciclo de ligações.
 *
 * @param recoveryWindow quanto tempo depois do horário ainda vale ligar (app fora do ar)
 * @param stuckAfter sem relatório do provedor depois disso, a ligação é dada como falha
 * @param maxDurationSeconds duração máxima de uma ligação
 * @param assistantName nome com que a assistente se apresenta
 */
@ConfigurationProperties("app.calls")
public record LigacaoProperties(
    @DefaultValue("PT2H") Duration recoveryWindow,
    @DefaultValue("PT30M") Duration stuckAfter,
    @DefaultValue("600") int maxDurationSeconds,
    @DefaultValue("Cuida") String assistantName) {}
