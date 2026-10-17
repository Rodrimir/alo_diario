package br.com.cuida.ligacao.model;

import java.util.UUID;

/** Publicado (outbox) quando o resumo de uma ligação fica pronto: hora de avisar a família. */
public record LigacaoProcessada(UUID callId, UUID elderlyId) {}
