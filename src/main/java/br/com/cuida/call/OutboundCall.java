package br.com.cuida.call;

import br.com.cuida.questionnaire.QuestionnaireSnapshot;
import java.util.UUID;

/**
 * Pedido de ligação montado a partir do idoso e do snapshot do questionário.
 *
 * @param callId nosso id, enviado como metadado e devolvido nos webhooks
 * @param phoneNumber destino em E.164
 * @param firstMessage abertura fixa, sempre com o aviso de gravação (não depende do LLM)
 * @param systemPrompt instruções da assistente para a conversa
 */
public record OutboundCall(
    UUID callId,
    String phoneNumber,
    String elderlyName,
    String firstMessage,
    String systemPrompt,
    QuestionnaireSnapshot questionnaire,
    int maxDurationSeconds) {}
