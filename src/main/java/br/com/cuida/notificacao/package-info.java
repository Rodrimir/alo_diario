/**
 * Avisos à família pelo WhatsApp: resumo do dia, "não conseguimos falar" e alerta urgente. O envio
 * passa pela API de WhatsApp da equipe (whats-api), com templates aprovados pela Meta.
 */
@ApplicationModule(
    displayName = "Notificação",
    allowedDependencies = {"infra", "ligacao", "ligacao :: model", "idoso", "idoso :: model"})
package br.com.cuida.notificacao;

import org.springframework.modulith.ApplicationModule;
