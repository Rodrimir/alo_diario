/**
 * Leitura do tutor: calendário do mês, ligações do dia, detalhe com resumo, transcrição, áudio e o
 * log de acessos. Toda leitura de dado sensível é auditada.
 */
@ApplicationModule(
    displayName = "Calendário",
    allowedDependencies = {
      "infra",
      "ligacao",
      "ligacao :: model",
      "idoso",
      "idoso :: model",
      "gravacao",
      "gravacao :: model",
      "auditoria",
      "auditoria :: model"
    })
package br.com.cuida.calendario;

import org.springframework.modulith.ApplicationModule;
