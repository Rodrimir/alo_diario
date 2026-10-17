/**
 * Ciclo de vida da ligação: agendador, retentativas, disparo, status e a porta {@code
 * VoiceCallGateway}. Adaptadores em {@code call.vapi} e {@code call.mock}.
 */
@ApplicationModule(
    displayName = "Call",
    allowedDependencies = {"shared", "elderly", "schedule", "questionnaire", "webhook"})
package br.com.cuida.call;

import org.springframework.modulith.ApplicationModule;
