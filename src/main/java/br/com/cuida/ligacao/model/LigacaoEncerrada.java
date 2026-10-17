package br.com.cuida.ligacao.model;

import java.util.UUID;

/**
 * Publicado (outbox) quando o provedor informa o fim da ligação. O pipeline de análise processa as
 * que terminaram como {@link StatusLigacao#COMPLETED}.
 */
public record LigacaoEncerrada(UUID callId, UUID elderlyId, StatusLigacao status) {}
