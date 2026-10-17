package br.com.cuida.idoso.model;

import java.util.UUID;

/**
 * Publicado (síncrono, na mesma transação) quando um idoso é cadastrado. Agenda e questionário
 * criam seus valores iniciais a partir dele.
 */
public record IdosoCadastrado(UUID elderlyId) {}
