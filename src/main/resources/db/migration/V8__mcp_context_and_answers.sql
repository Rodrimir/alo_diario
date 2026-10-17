-- =============================================================================
-- V8 - Contexto da IA de voz (MCP) e o que ela registra durante a ligação.
--   elderly_skills  : skills da pessoa (escritas pela família) e skills da
--                     biblioteca fixadas para o idoso, no formato SKILL.md
--   family_requests : pedidos pontuais da família ("pergunte se marcou o médico")
--   call_answers    : respostas, recado e pedidos registrados pela IA na ligação
--   calls           : alerta sinalizado durante a ligação e fim da conversa
-- =============================================================================

create table elderly_skills (
    id          uuid          primary key,
    elderly_id  uuid          not null references elderly (id) on delete cascade,
    source      varchar(20)   not null,
    name        varchar(64)   not null,
    description varchar(300)  not null,
    body        text,
    active      boolean       not null default true,
    created_at  timestamptz   not null,
    updated_at  timestamptz   not null,
    constraint ux_elderly_skills_name unique (elderly_id, name),
    constraint ck_elderly_skills_source check (source in ('LIBRARY', 'CUSTOM')),
    constraint ck_elderly_skills_name check (name ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    constraint ck_elderly_skills_body check (source = 'LIBRARY' or body is not null)
);

create table family_requests (
    id                uuid          primary key,
    elderly_id        uuid          not null references elderly (id) on delete cascade,
    text              varchar(300)  not null,
    starts_on         date          not null,
    ends_on           date,
    status            varchar(20)   not null,
    last_call_id      uuid          references calls (id) on delete set null,
    answer            varchar(500),
    answered_at       timestamptz,
    created_at        timestamptz   not null,
    updated_at        timestamptz   not null,
    constraint ck_family_requests_status check (status in ('PENDING', 'ANSWERED', 'CANCELED')),
    constraint ck_family_requests_dates check (ends_on is null or ends_on >= starts_on)
);

create index ix_family_requests_elderly on family_requests (elderly_id, status, starts_on);

create table call_answers (
    id            uuid          primary key,
    call_id       uuid          not null references calls (id) on delete cascade,
    attribute_key varchar(60)   not null,
    question      varchar(300),
    answer        varchar(500)  not null,
    status        varchar(20)   not null,
    recorded_at   timestamptz   not null,
    constraint ux_call_answers_key unique (call_id, attribute_key),
    constraint ck_call_answers_status check (status in ('OK', 'ATTENTION', 'UNKNOWN'))
);

alter table calls
    add column realtime_alert_level     varchar(20),
    add column realtime_alert_reason    varchar(300),
    add column conversation_completed_at timestamptz,
    add constraint ck_calls_realtime_alert
        check (realtime_alert_level is null or realtime_alert_level in ('ATTENTION', 'URGENT'));
