package br.com.cuida.gravacao.model;

/**
 * Gravação copiada para o nosso armazenamento.
 *
 * @param objectKey chave no storage: {@code elderly/{elderlyId}/calls/{callId}.{ext}}
 * @param contentType tipo MIME
 * @param sizeBytes tamanho em bytes
 */
public record GravacaoArmazenada(String objectKey, String contentType, long sizeBytes) {}
