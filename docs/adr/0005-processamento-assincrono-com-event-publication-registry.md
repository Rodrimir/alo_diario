# ADR 0005 — Processamento assíncrono com o Event Publication Registry do Spring Modulith

- **Status:** aceito
- **Data:** 2026-10-17

## Contexto

O webhook precisa responder rápido, mas o pós-processamento (baixar gravação, salvar no S3,
transcrever, resumir) leva de segundos a minutos e depende de serviços externos. Perder esse
trabalho num restart deixaria o dia do idoso sem resumo.

## Opções

1. `@Async` puro com executor dedicado: simples, mas perde o trabalho em crash ou deploy.
2. Fila em tabela feita à mão: durável, mas exige polling, `SKIP LOCKED`, backoff e limpeza.
3. **Event Publication Registry (JDBC) do Spring Modulith:** outbox transacional pronto.

## Decisão

Opção 3, com um **executor dedicado e limitado** (`applicationTaskExecutor`):

- O evento (ex.: `LigacaoEncerrada`) é gravado em `event_publication` **na mesma transação** que muda o
  estado. O listener roda depois do commit, em outra thread.
- Publicações incompletas são **republicadas no restart**. Um job (com ShedLock) reenvia as que
  ficarem paradas por mais de N minutos.
- Os eventos carregam **apenas ids**, nunca transcrição ou telefone.
- Cada etapa do pipeline é **idempotente** e as entidades usam `@Version`, já que o
  reprocessamento pode acontecer mais de uma vez.

## Consequências

- Já usamos o Modulith, então nenhuma infraestrutura nova (sem broker).
- A tabela `event_publication` precisa de limpeza periódica das publicações concluídas.
- Se o volume crescer muito, a externalização de eventos do Modulith permite migrar para
  Kafka/RabbitMQ/SQS sem reescrever os listeners.
