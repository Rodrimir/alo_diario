# ADR 0003 — JSONB para o resumo estruturado e o snapshot do questionário

- **Status:** aceito
- **Data:** 2026-10-17

## Contexto

O resumo de cada ligação (`CallSummary`) tem estrutura aninhada (lista de respostas, menções,
motivos de alerta) e deve evoluir sem migrations pesadas. O questionário muda ao longo do tempo,
mas cada ligação precisa lembrar exatamente quais perguntas estavam valendo.

## Decisão

- `calls.structured_summary` e `calls.questionnaire_snapshot` são colunas **`jsonb`**, mapeadas
  com `@JdbcTypeCode(SqlTypes.JSON)` para `record`s Java. Não usamos a lib `hibernate-types`.
- A serialização usa o `Jackson3JsonFormatMapper` nativo do Hibernate 7, configurado com o mesmo
  `JsonMapper` (Jackson 3) da API (`shared.persistence.PersistenceConfig`).
- Campos consultados com frequência ficam **desnormalizados em colunas** (`alert_level`,
  `summary_text`) para filtros e índices simples. Há também um índice GIN (`jsonb_path_ops`) no
  resumo para buscas futuras.
- O snapshot é gravado na criação da ligação. Assim, `PUT /questionnaire` pode substituir a lista
  inteira sem perder o histórico.

## Consequências

- Esquema do resumo versionado pelo código: mudanças incompatíveis exigem tolerância na leitura
  ou uma migration de dados.
- Validação do conteúdo acontece na aplicação (Jakarta Validation + regras do resumidor), não no
  banco.
