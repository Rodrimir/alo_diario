-- =============================================================================
-- V6 - Responsável que recebe o resumo diário (nome e WhatsApp), no cadastro do
--      idoso. Pode ser diferente de quem tem a conta (o tutor).
-- =============================================================================

alter table elderly
    add column guardian_name     varchar(120),
    add column guardian_whatsapp varchar(16),
    add constraint ck_elderly_guardian_whatsapp_e164
        check (guardian_whatsapp is null or guardian_whatsapp ~ '^\+[1-9][0-9]{7,14}$');
