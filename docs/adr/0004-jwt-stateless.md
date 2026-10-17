# ADR 0004 — Autenticação stateless com JWT (HS256)

- **Status:** aceito
- **Data:** 2026-10-17

## Contexto

A API será consumida por um frontend (web/mobile) ainda inexistente. Queremos escalar
horizontalmente sem sessão em memória e sem adicionar um servidor de identidade no MVP.

## Decisão

- Spring Security 7, `SessionCreationPolicy.STATELESS`, CSRF desabilitado (API sem cookies).
- Tokens **JWT HS256** emitidos pela própria aplicação (`NimbusJwtEncoder`) e validados pelo
  `spring-boot-starter-security-oauth2-resource-server` (`NimbusJwtDecoder`). O segredo vem de
  `JWT_SECRET` (mínimo de 32 bytes; a aplicação não sobe com segredo curto).
- Claims: `sub` (id do usuário), `roles`, `iat`, `exp` (padrão 1h).
- Senhas com BCrypt (limite de 72 bytes validado na entrada).
- Recurso de outro tutor responde **404**, não 403, para não revelar que ele existe.

## Consequências

- Sem revogação antes do `exp` (logout é só do lado do cliente) e sem refresh token no MVP.
- Rotacionar o segredo invalida todos os tokens. Se precisarmos de chaves por ambiente ou de
  rotação suave, migramos para RS256 com JWKS ou para um IdP (Keycloak, Cognito, Auth0).
