package br.com.cuida.gravacao.model;

import java.io.InputStream;

/**
 * Conteúdo de um áudio armazenado.
 *
 * @param stream conteúdo (quem lê fecha)
 * @param contentLength tamanho em bytes
 * @param contentType tipo MIME (ex.: {@code audio/mpeg})
 */
public record ConteudoAudio(InputStream stream, long contentLength, String contentType) {}
