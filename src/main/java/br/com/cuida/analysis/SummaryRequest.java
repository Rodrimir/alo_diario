package br.com.cuida.analysis;

import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import java.time.LocalDate;

/**
 * Entrada do resumidor. O nome da pessoa não vai para o provedor de IA (minimização de dados).
 *
 * @param questionnaire perguntas vigentes na ligação
 * @param transcript transcrição completa
 * @param date dia da ligação no fuso do idoso
 */
public record SummaryRequest(
    QuestionnaireSnapshot questionnaire, String transcript, LocalDate date) {}
