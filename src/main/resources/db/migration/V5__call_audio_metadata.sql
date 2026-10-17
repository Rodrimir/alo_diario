-- =============================================================================
-- V5 - Metadados do áudio guardado (como mídia no exemplo: tipo e tamanho na
--      própria linha) e marca de expurgo pela retenção.
-- =============================================================================

alter table calls
    add column audio_mime_type  varchar(100),
    add column audio_size_bytes bigint,
    add column audio_purged_at  timestamptz,
    add constraint ck_calls_audio_size check (audio_size_bytes is null or audio_size_bytes >= 0);

-- Retenção: áudios ainda guardados, por data de término.
create index ix_calls_audio_retention on calls (ended_at)
    where audio_object_key is not null;
