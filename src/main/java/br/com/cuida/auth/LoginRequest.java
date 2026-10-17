package br.com.cuida.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /api/login}. */
record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
