/**
 * Ciclo de vida da ligação: agendador, retentativas, disparo, status e a porta {@code
 * GatewayDeVoz}. Adaptadores em {@code ligacao.vapi} e {@code ligacao.mock}.
 */
@ApplicationModule(
    displayName = "Ligação",
    allowedDependencies = {
      "infra",
      "idoso",
      "idoso :: model",
      "agenda",
      "agenda :: model",
      "questionario",
      "questionario :: model",
      "webhook",
      "webhook :: model"
    })
package br.com.cuida.ligacao;

import org.springframework.modulith.ApplicationModule;
