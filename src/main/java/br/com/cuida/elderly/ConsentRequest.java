package br.com.cuida.elderly;

import jakarta.validation.constraints.NotNull;

/** Corpo de {@code PUT /api/elderly/{id}/consent}. */
record ConsentRequest(@NotNull Boolean recordingConsent) {}
