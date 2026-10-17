-- =============================================================================
-- V1 - Esquema inicial do MVP.
-- Convenções: ids UUID gerados pela aplicação; datas em timestamptz (UTC);
-- enums como varchar + CHECK; JSON em jsonb.
-- =============================================================================

-- ---------------------------------------------------------------- auth ------
create table users (
    id            uuid         primary key,
    name          varchar(120) not null,
    email         varchar(254) not null,
    password_hash varchar(100) not null,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    constraint ux_users_email unique (email),
    constraint ck_users_email_lowercase check (email = lower(email))
);

create table user_roles (
    user_id uuid        not null references users (id) on delete cascade,
    role    varchar(30) not null,
    primary key (user_id, role),
    constraint ck_user_roles_role check (role in ('ROLE_TUTOR', 'ROLE_ADMIN'))
);

-- ------------------------------------------------------------- elderly ------
create table elderly (
    id                  uuid          primary key,
    tutor_id            uuid          not null references users (id),
    name                varchar(120)  not null,
    preferred_name      varchar(60),
    phone_number        varchar(16)   not null,
    timezone            varchar(64)   not null,
    notes               varchar(2000),
    recording_consent   boolean       not null default false,
    consent_recorded_at timestamptz,
    consent_recorded_by uuid          references users (id) on delete set null,
    active              boolean       not null default true,
    created_at          timestamptz   not null,
    updated_at          timestamptz   not null,
    constraint ck_elderly_phone_e164 check (phone_number ~ '^\+[1-9][0-9]{7,14}$')
);

create index ix_elderly_tutor on elderly (tutor_id);

-- ------------------------------------------------------------ schedule ------
create table call_schedules (
    id                     uuid        primary key,
    elderly_id             uuid        not null references elderly (id) on delete cascade,
    local_time             time        not null,
    days_of_week           jsonb       not null,
    active                 boolean     not null default false,
    max_attempts           integer     not null default 3,
    retry_interval_minutes integer     not null default 20,
    created_at             timestamptz not null,
    updated_at             timestamptz not null,
    constraint ux_call_schedules_elderly unique (elderly_id),
    constraint ck_call_schedules_days check (jsonb_typeof(days_of_week) = 'array'),
    constraint ck_call_schedules_attempts check (max_attempts between 1 and 5),
    constraint ck_call_schedules_interval check (retry_interval_minutes between 5 and 240)
);

-- ------------------------------------------------------- questionnaire ------
create table questionnaire_items (
    id            uuid         primary key,
    elderly_id    uuid         not null references elderly (id) on delete cascade,
    attribute_key varchar(40)  not null,
    question_text varchar(200) not null,
    answer_type   varchar(20)  not null,
    position      integer      not null,
    active        boolean      not null default true,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    constraint ux_questionnaire_items_key unique (elderly_id, attribute_key),
    constraint ck_questionnaire_items_key check (attribute_key ~ '^[a-z][a-z0-9_]{1,39}$'),
    constraint ck_questionnaire_items_type check (answer_type in ('YES_NO', 'SCALE_1_5', 'FREE_TEXT')),
    constraint ck_questionnaire_items_position check (position >= 0)
);

create index ix_questionnaire_items_elderly on questionnaire_items (elderly_id, position);

-- ---------------------------------------------------------------- call ------
create table calls (
    id                     uuid         primary key,
    elderly_id             uuid         not null references elderly (id) on delete cascade,
    trigger_type           varchar(20)  not null,
    scheduled_for          timestamptz  not null,
    local_date             date         not null,
    attempt                integer      not null,
    status                 varchar(30)  not null,
    provider               varchar(20),
    provider_call_id       varchar(100),
    started_at             timestamptz,
    ended_at               timestamptz,
    duration_seconds       integer,
    end_reason             varchar(120),
    provider_recording_url text,
    audio_object_key       varchar(300),
    transcript             text,
    transcript_source      varchar(20),
    questionnaire_snapshot jsonb,
    structured_summary     jsonb,
    summary_text           text,
    alert_level            varchar(20),
    failure_reason         varchar(200),
    version                bigint       not null default 0,
    created_at             timestamptz  not null,
    updated_at             timestamptz  not null,
    constraint ux_calls_provider_call_id unique (provider_call_id),
    constraint ck_calls_trigger check (trigger_type in ('SCHEDULED', 'RETRY', 'MANUAL')),
    constraint ck_calls_attempt check (attempt >= 1),
    constraint ck_calls_duration check (duration_seconds is null or duration_seconds >= 0),
    constraint ck_calls_status check (status in (
        'SCHEDULED', 'DIALING', 'IN_PROGRESS', 'COMPLETED', 'NO_ANSWER', 'BUSY', 'FAILED',
        'PROCESSING', 'PROCESSED', 'PROCESSING_FAILED')),
    constraint ck_calls_transcript_source check (transcript_source in ('PROVIDER', 'WHISPER')),
    constraint ck_calls_alert_level check (alert_level in ('NONE', 'ATTENTION', 'URGENT'))
);

-- Calendário do tutor.
create index ix_calls_elderly_date on calls (elderly_id, local_date);
-- Despacho do agendador (status + horário) e varreduras por status.
create index ix_calls_status_scheduled on calls (status, scheduled_for);
-- Busca dentro do resumo estruturado.
create index ix_calls_structured_summary on calls using gin (structured_summary jsonb_path_ops);
-- Nunca duas tentativas automáticas iguais no mesmo dia (defesa além do ShedLock).
create unique index ux_calls_daily_attempt on calls (elderly_id, local_date, attempt)
    where trigger_type <> 'MANUAL';

-- ------------------------------------------------------------- webhook ------
create table webhook_events (
    id           uuid          primary key,
    provider     varchar(20)   not null,
    event_id     varchar(200)  not null,
    event_type   varchar(60),
    payload      jsonb,
    received_at  timestamptz   not null,
    processed_at timestamptz,
    error        varchar(1000),
    constraint ux_webhook_events_provider_event unique (provider, event_id)
);

create index ix_webhook_events_unprocessed on webhook_events (received_at)
    where processed_at is null;

-- --------------------------------------------------------------- audit ------
create table access_audit_logs (
    id          uuid        primary key,
    user_id     uuid        references users (id) on delete set null,
    elderly_id  uuid        references elderly (id) on delete set null,
    call_id     uuid        references calls (id) on delete set null,
    action      varchar(30) not null,
    ip          varchar(45),
    occurred_at timestamptz not null,
    constraint ck_access_audit_logs_action check (action in ('VIEW_SUMMARY', 'VIEW_TRANSCRIPT', 'PLAY_AUDIO'))
);

create index ix_access_audit_logs_elderly on access_audit_logs (elderly_id, occurred_at);
create index ix_access_audit_logs_call on access_audit_logs (call_id);

-- ----------------------------------------------------- infraestrutura ------
-- ShedLock (lock distribuído dos jobs agendados).
create table shedlock (
    name       varchar(64)  not null primary key,
    lock_until timestamptz  not null,
    locked_at  timestamptz  not null,
    locked_by  varchar(255) not null
);

-- Spring Modulith - Event Publication Registry (schema v2 do spring-modulith-events-jdbc 2.1.1).
create table event_publication (
    id                     uuid        not null primary key,
    listener_id            text        not null,
    event_type             text        not null,
    serialized_event       text        not null,
    publication_date       timestamptz not null,
    completion_date        timestamptz,
    status                 text,
    completion_attempts    int,
    last_resubmission_date timestamptz
);

create index event_publication_serialized_event_hash_idx on event_publication using hash (serialized_event);
create index event_publication_by_completion_date_idx on event_publication (completion_date);
