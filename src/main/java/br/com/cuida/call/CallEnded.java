package br.com.cuida.call;

import java.util.UUID;

/**
 * Publicado (outbox) quando o provedor informa o fim da ligação. O pipeline de análise processa as
 * que terminaram como {@link CallStatus#COMPLETED}.
 */
public record CallEnded(UUID callId, UUID elderlyId, CallStatus status) {}
