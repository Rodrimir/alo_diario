/**
 * Pipeline pós-ligação: copia a gravação, transcreve (se preciso) e gera o resumo estruturado
 * (portas {@code Transcriber}/{@code Summarizer} com adaptadores OpenAI e mock).
 */
@ApplicationModule(
    displayName = "Analysis",
    allowedDependencies = {"shared", "call", "recording", "questionnaire"})
package br.com.cuida.analysis;

import org.springframework.modulith.ApplicationModule;
