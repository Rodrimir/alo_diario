package br.com.cuida.elderly;

import java.util.UUID;

/**
 * Publicado (síncrono, na mesma transação) quando um idoso é cadastrado. Agenda e questionário
 * criam seus valores iniciais a partir dele.
 */
public record ElderlyRegistered(UUID elderlyId) {}
