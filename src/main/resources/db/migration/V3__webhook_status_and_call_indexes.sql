-- =============================================================================
-- V3 - Situação de cada webhook recebido (gravado antes de processar) e índices
--      do agendador de ligações.
-- =============================================================================

alter table webhook_events
    add column status   varchar(20) not null default 'PENDING',
    add column attempts integer     not null default 0;

update webhook_events set status = 'PROCESSED' where processed_at is not null;

alter table webhook_events
    add constraint ck_webhook_events_status
        check (status in ('PENDING', 'PROCESSED', 'FAILED', 'IGNORED'));

drop index ix_webhook_events_unprocessed;
-- Reprocessamento dos que falharam e expurgo de payloads antigos.
create index ix_webhook_events_status on webhook_events (status, received_at)
    where status in ('PENDING', 'FAILED');

-- Ligações ainda em andamento de um idoso (409 no disparo manual) e varredura de travadas.
create index ix_calls_elderly_status on calls (elderly_id, status);
