package br.com.cuida.analise.model;

import br.com.cuida.questionario.model.QuestionarioSnapshot;
import java.time.LocalDate;

/**
 * Entrada do resumidor. O nome da pessoa não vai para o provedor de IA (minimização de dados).
 *
 * @param questionnaire perguntas vigentes na ligação
 * @param transcript transcrição completa
 * @param date dia da ligação no fuso do idoso
 */
public record PedidoDeResumo(
    QuestionarioSnapshot questionnaire, String transcript, LocalDate date) {}
