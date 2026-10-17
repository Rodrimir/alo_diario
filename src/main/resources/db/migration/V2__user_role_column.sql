-- =============================================================================
-- V2 - Papel do usuário em coluna única (users.role = 'TUTOR' | 'ADMIN'),
-- no lugar da tabela user_roles. Cada usuário tem exatamente um papel.
-- =============================================================================

alter table users add column role varchar(20);

update users u
   set role = case
                when exists (select 1 from user_roles r
                              where r.user_id = u.id and r.role = 'ROLE_ADMIN') then 'ADMIN'
                else 'TUTOR'
              end;

alter table users alter column role set not null;
alter table users add constraint ck_users_role check (role in ('TUTOR', 'ADMIN'));

drop table user_roles;
