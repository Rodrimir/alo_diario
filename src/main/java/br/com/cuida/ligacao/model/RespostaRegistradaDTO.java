package br.com.cuida.ligacao.model;

/**
 * Resposta registrada na ligação.
 *
 * @param attributeKey chave da pergunta, {@code message_for_family} para o recado ou {@code
 *     request:<id>} para um pedido pontual da família
 * @param question texto da pergunta ou do pedido (para pedidos, que não estão no snapshot)
 */
public record RespostaRegistradaDTO(
    String attributeKey, String question, String answer, StatusResposta status) {}
