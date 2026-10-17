/**
 * Pipeline pós-ligação: copia a gravação, transcreve (se preciso) e gera o resumo estruturado
 * (portas {@code Transcritor}/{@code Resumidor} com adaptadores OpenAI e simulados).
 */
@ApplicationModule(
    displayName = "Análise",
    allowedDependencies = {
      "infra",
      "ligacao",
      "ligacao :: model",
      "gravacao",
      "gravacao :: model",
      "questionario :: model"
    })
package br.com.cuida.analise;

import org.springframework.modulith.ApplicationModule;
