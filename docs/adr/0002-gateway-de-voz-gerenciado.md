# ADR 0002 — Gateway de voz gerenciado (Vapi) atrás de uma porta

- **Status:** aceito
- **Data:** 2026-10-17

## Contexto

Conversar por voz em tempo real exige telefonia, streaming de áudio, detecção de fala/turnos,
TTS/STT de baixa latência e gravação. Implementar isso no servidor Java seria caro e arriscado
para um MVP.

## Decisão

- A conversa é delegada a um **gateway de voz com IA** (padrão: **Vapi**), que liga, conversa,
  grava e, ao fim, nos chama por **webhook** com status, motivo do encerramento, duração,
  transcrição e URL da gravação.
- O domínio depende só da porta `VoiceCallGateway` (`startCall(CallRequest) → providerCallId`).
  Adaptadores: `VapiVoiceCallGateway` (produção) e `MockVoiceCallGateway` (dev/test), que
  simula a ligação e injeta o mesmo fluxo do webhook.
- O assistente é **transitório** (montado a cada chamada) a partir das perguntas do idoso. O
  aviso de gravação vai no `firstMessage`, fixo, e não depende do LLM.
- Como o Vapi **não envia as credenciais da organização para assistentes transitórios**, o
  segredo do webhook vai em `assistant.server.headers` e é validado por um `WebhookVerifier`.
- O webhook é **idempotente** (`webhook_events` com `(provider, event_id)` único). O Vapi não
  envia id de evento, então o id é derivado (`{callId}:{type}[:{status}]`).
- `startCall` **não tem retry automático**: um POST que estoura o timeout pode já ter criado a
  ligação, e repetir faria o idoso receber duas chamadas.

## Consequências

- Trocar de provedor (Retell, Bland, Twilio + LLM próprio…) é escrever um novo adaptador.
- Ficamos dependentes da disponibilidade, do preço por minuto e da retenção de dados do Vapi.
  Veja as questões em aberto no README.
