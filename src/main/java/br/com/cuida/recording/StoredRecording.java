package br.com.cuida.recording;

/**
 * Gravação copiada para o nosso armazenamento.
 *
 * @param objectKey chave no storage: {@code elderly/{elderlyId}/calls/{callId}.{ext}}
 * @param contentType tipo MIME
 * @param sizeBytes tamanho em bytes
 */
public record StoredRecording(String objectKey, String contentType, long sizeBytes) {}
