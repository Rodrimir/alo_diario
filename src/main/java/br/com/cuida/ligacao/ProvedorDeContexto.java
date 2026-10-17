package br.com.cuida.ligacao;

import br.com.cuida.idoso.model.IdosoDTO;
import br.com.cuida.questionario.model.QuestionarioSnapshot;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Porta para o contexto da IA de voz montado antes da ligação (regras base, skills da pessoa e
 * pedidos da família). Implementada pelo módulo {@code mcp}; sem ela, a ligação usa as instruções
 * padrão do {@link RoteiroDaConversa}.
 */
public interface ProvedorDeContexto {

  /** Instruções completas para a sessão de voz desta ligação. */
  String montar(UUID callId, IdosoDTO idoso, QuestionarioSnapshot perguntas, LocalDate dia);
}
