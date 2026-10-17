/** Perguntas que a IA deve cobrir na conversa e o snapshot congelado em cada ligação. */
@ApplicationModule(
    displayName = "Questionário",
    allowedDependencies = {"infra", "idoso", "idoso :: model"})
package br.com.cuida.questionario;

import org.springframework.modulith.ApplicationModule;
