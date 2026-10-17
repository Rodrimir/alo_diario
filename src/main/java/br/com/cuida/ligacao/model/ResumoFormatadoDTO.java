package br.com.cuida.ligacao.model;

import java.util.List;

/**
 * Resumo pronto para mostrar ou enviar à família.
 *
 * @param checklist uma linha por pergunta (sem a do recado)
 * @param familyMessage recado da pessoa ({@code null} se não houver)
 * @param report texto completo no formato do pitch, com ✅/⚠️/❔ e o recado
 */
public record ResumoFormatadoDTO(
    List<ItemChecklistDTO> checklist, String familyMessage, String report) {}
