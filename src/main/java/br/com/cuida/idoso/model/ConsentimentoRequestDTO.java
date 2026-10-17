package br.com.cuida.idoso.model;

import jakarta.validation.constraints.NotNull;

/** Corpo de {@code PUT /api/elderly/{id}/consent}. */
public record ConsentimentoRequestDTO(@NotNull Boolean recordingConsent) {}
