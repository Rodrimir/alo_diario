-- =============================================================================
-- V4 - ShedLock com usingDbTime() grava timezone('utc', now()), um timestamp
--      SEM fuso. Em coluna timestamptz o valor era reinterpretado no fuso da
--      sessão (deslocado em -03:00). As colunas passam a ser timestamp em UTC,
--      como recomenda a documentação do ShedLock.
-- =============================================================================

alter table shedlock
    alter column lock_until type timestamp using lock_until at time zone 'UTC',
    alter column locked_at  type timestamp using locked_at  at time zone 'UTC';
