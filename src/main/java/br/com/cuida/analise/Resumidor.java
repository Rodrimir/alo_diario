package br.com.cuida.analise;

import br.com.cuida.analise.model.PedidoDeResumo;
import br.com.cuida.ligacao.model.ResumoLigacao;

/** Porta do resumidor: transcrição + perguntas → resumo estruturado. */
public interface Resumidor {

  ResumoLigacao summarize(PedidoDeResumo request);
}
