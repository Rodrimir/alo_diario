# ADR 0007 — Contrato da API e armazenamento no padrão do whats-api

- **Status:** aceito
- **Data:** 2026-10-17

## Contexto

A equipe já mantém outra API (`exemplos/whats-api`, não versionada aqui) e os clientes/frontends
dela já conhecem um formato de entrada e saída. Para o Cuida ser consumido do mesmo jeito, o
contrato HTTP e a forma de guardar os dados seguem aquele projeto. O **código** não foi copiado
(lá é JDBC manual, interceptors e OpenAPI escrito à mão); só o que sai e como é armazenado.

## Decisão

Seguimos do exemplo:

- Rotas sob `/api`, autenticação em `POST /api/login` e `POST /api/register` devolvendo
  `{token, ...dados do usuário}`, `GET /api/ping` → `{"status": "ok"}` sem autenticação.
- Recursos aninhados (`/api/elderly/{id}/schedule`, como `/api/setores/{id}/membros`).
- Corpo e resposta em JSON camelCase a partir de `record`s; enums como texto em maiúsculas; ids
  UUID; listas como **array puro** (sem envelope de paginação).
- Criação responde **200** com o recurso; `DELETE` responde **200 sem corpo**; ação sem retorno,
  204.
- Erros: `{"status": 404, "message": "..."}`; validação: `{"status": 400, "errors": [{"field",
  "message"}]}`. Recurso de outro dono responde **404**, não 403.
- Datas como `LocalDateTime` sem offset (`"2026-10-17T09:30:00"`).
- Webhook fora de `/api` (`/webhook/vapi`), responde `EVENT_RECEIVED`; 401 sem corpo quando a
  assinatura/segredo não confere. Todo evento é **gravado antes de ser processado** e reprocessado
  se falhar (o "garantidor" do exemplo), com `status` na própria linha.
- Mídia servida pela própria API (`GET .../audio`), com `Content-Type` e
  `Content-Disposition: inline`, e tipo/tamanho guardados na linha da ligação.
- Banco: UUID gerado pela aplicação, `snake_case`, enums em `varchar`, papel em coluna única
  (`users.role` = `TUTOR`/`ADMIN`).

Diferenças conscientes:

- Datas são **gravadas** em `timestamptz` (UTC), não em `timestamp`, porque cada idoso tem o seu
  fuso. Na saída viram `LocalDateTime` **no fuso do idoso**, que vem no cadastro (`timezone`).
- O erro de validação também traz `message` ("Dados inválidos"), além de `errors`.
- `PUT` devolve o recurso atualizado (no exemplo devolve 200 vazio), o que poupa um GET ao
  frontend.
- Duplicidade (ex.: e-mail já cadastrado) é **409**, não 400; o erro 500 nunca expõe a mensagem
  da exceção.
- O áudio não pode ir para link público: nada de URL pré-assinada.

## Consequências

- Um frontend que já fala com o whats-api reaproveita o tratamento de erros, login e listas.
- Sem paginação: listas crescem com o uso (ex.: idosos de um administrador). Se virar problema,
  entra um `?quant=` como o `GET /api/messages` do exemplo.
- O streaming do áudio passa pelo backend (consome banda e conexão do servidor), em troca de
  controle de acesso e auditoria a cada reprodução.
