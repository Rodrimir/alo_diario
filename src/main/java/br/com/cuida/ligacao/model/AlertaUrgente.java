package br.com.cuida.ligacao.model;

import java.util.UUID;

/**
 * Publicado (outbox) quando a IA sinaliza um alerta urgente <b>durante</b> a ligação (queda, falta
 * de ar, pedido de ajuda): a família é avisada na hora, sem esperar o resumo.
 */
public record AlertaUrgente(UUID callId, UUID elderlyId, String motivo) {}
