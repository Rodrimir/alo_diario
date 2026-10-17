package br.com.cuida.ligacao.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Publicado (outbox) uma única vez quando a última tentativa automática do dia termina sem falar
 * com a pessoa (não atendeu, ocupado ou falha técnica).
 *
 * @param lastCallId última tentativa do dia
 * @param attempts quantas tentativas foram feitas
 */
public record TentativasEsgotadas(
    UUID elderlyId, LocalDate localDate, int attempts, UUID lastCallId) {}
