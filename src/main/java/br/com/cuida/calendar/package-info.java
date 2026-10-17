/** Consultas de leitura do tutor: calendário, detalhe, transcrição e URL do áudio. */
@ApplicationModule(
    displayName = "Calendar",
    allowedDependencies = {"shared", "call", "elderly", "recording", "audit"})
package br.com.cuida.calendar;

import org.springframework.modulith.ApplicationModule;
