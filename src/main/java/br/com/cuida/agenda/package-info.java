/** Agenda de ligações de cada pessoa idosa (horário local, dias da semana, retentativas). */
@ApplicationModule(
    displayName = "Agenda",
    allowedDependencies = {"infra", "idoso", "idoso :: model"})
package br.com.cuida.agenda;

import org.springframework.modulith.ApplicationModule;
