package br.com.cuida.analysis;

import br.com.cuida.call.CallSummary;

/** Porta do resumidor: transcrição + perguntas → resumo estruturado. */
public interface Summarizer {

  CallSummary summarize(SummaryRequest request);
}
