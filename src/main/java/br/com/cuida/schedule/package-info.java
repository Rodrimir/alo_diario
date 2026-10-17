/** Agenda de ligações de cada pessoa idosa (horário local, dias da semana, retentativas). */
@ApplicationModule(
    displayName = "Schedule",
    allowedDependencies = {"shared", "elderly"})
package br.com.cuida.schedule;

import org.springframework.modulith.ApplicationModule;
