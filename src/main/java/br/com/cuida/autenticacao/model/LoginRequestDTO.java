package br.com.cuida.autenticacao.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /api/login}. */
public record LoginRequestDTO(@NotBlank @Email String email, @NotBlank String password) {}
