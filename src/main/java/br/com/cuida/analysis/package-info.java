/**
 * Pipeline pós-ligação: transcrição e resumo estruturado com IA (portas + adaptadores Spring AI e
 * mock).
 */
@ApplicationModule(
    displayName = "Analysis",
    allowedDependencies = {"shared", "call", "recording"})
package br.com.cuida.analysis;

import org.springframework.modulith.ApplicationModule;
