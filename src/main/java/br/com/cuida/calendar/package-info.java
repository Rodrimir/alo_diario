/**
 * Leitura do tutor: calendário do mês, ligações do dia, detalhe com resumo, transcrição, áudio e o
 * log de acessos. Toda leitura de dado sensível é auditada.
 */
@ApplicationModule(
    displayName = "Calendar",
    allowedDependencies = {"shared", "call", "elderly", "recording", "audit"})
package br.com.cuida.calendar;

import org.springframework.modulith.ApplicationModule;
