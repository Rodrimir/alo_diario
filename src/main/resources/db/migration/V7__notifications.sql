-- =============================================================================
-- V7 - Avisos à família pelo WhatsApp (resumo do dia, sem contato, alerta) e a
--      marca que impede o planejador de reavaliar tentativas já encerradas.
-- =============================================================================

alter table calls add column retry_closed boolean not null default false;

create table notifications (
    id                  uuid          primary key,
    elderly_id          uuid          not null references elderly (id) on delete cascade,
    call_id             uuid          references calls (id) on delete cascade,
    kind                varchar(20)   not null,
    channel             varchar(20)   not null,
    recipient           varchar(16),
    template            varchar(100)  not null,
    status              varchar(20)   not null,
    content             text          not null,
    variables           jsonb         not null,
    provider_message_id varchar(200),
    attempts            integer       not null default 0,
    error               varchar(500),
    dedup_key           varchar(200)  not null,
    sent_at             timestamptz,
    created_at          timestamptz   not null,
    updated_at          timestamptz   not null,
    constraint ux_notifications_dedup unique (dedup_key),
    constraint ck_notifications_kind check (kind in ('CALL_SUMMARY', 'NO_CONTACT', 'URGENT_ALERT')),
    constraint ck_notifications_status check (status in ('PENDING', 'SENT', 'FAILED', 'SKIPPED')),
    constraint ck_notifications_attempts check (attempts >= 0)
);

create index ix_notifications_call on notifications (call_id, created_at);
create index ix_notifications_retry on notifications (status, updated_at) where status = 'FAILED';
