package br.com.cuida.ligacao.model;

/**
 * Uma linha do checklist do resumo.
 *
 * @param attributeKey chave da pergunta
 * @param status ✅/⚠️/❔
 * @param text frase curta (ex.: "Relatou dor no joelho")
 */
public record ItemChecklistDTO(String attributeKey, StatusResposta status, String text) {}
