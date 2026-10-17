package br.com.cuida.calendario.model;

import br.com.cuida.ligacao.model.ItemChecklistDTO;
import br.com.cuida.ligacao.model.NivelAlerta;
import br.com.cuida.ligacao.model.OrigemLigacao;
import br.com.cuida.ligacao.model.ResumoLigacao;
import br.com.cuida.ligacao.model.StatusLigacao;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detalhe de uma ligação: dados da tentativa e o resumo estruturado completo.
 *
 * @param summary resumo ({@code null} enquanto não processada)
 * @param audioUrl caminho do áudio nesta API ({@code null} se não houver)
 * @param audioPurged o áudio existiu, mas a retenção já o apagou
 * @param checklist uma linha ✅/⚠️/❔ por pergunta ({@code null} enquanto não processada)
 * @param familyMessage recado da pessoa para a família
 * @param report resumo pronto no formato do pitch (o mesmo texto enviado à família)
 */
public record DetalheLigacaoResponseDTO(
    UUID id,
    UUID elderlyId,
    OrigemLigacao trigger,
    StatusLigacao status,
    LocalDate localDate,
    int attempt,
    LocalDateTime scheduledFor,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Integer durationSeconds,
    String endReason,
    String failureReason,
    NivelAlerta alertLevel,
    String summaryText,
    ResumoLigacao summary,
    boolean hasTranscript,
    String audioUrl,
    boolean audioPurged,
    List<ItemChecklistDTO> checklist,
    String familyMessage,
    String report) {}
