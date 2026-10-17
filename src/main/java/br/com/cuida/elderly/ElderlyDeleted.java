package br.com.cuida.elderly;

import java.util.UUID;

/**
 * Publicado quando um idoso é excluído definitivamente (LGPD). O banco apaga em cascata agenda,
 * questionário e ligações; quem guarda dados fora do banco (áudios) limpa a partir deste evento.
 */
public record ElderlyDeleted(UUID elderlyId) {}
