# ADR 0006 — Spring Boot 4.1 + Spring AI 2.0

- **Status:** aceito (decidido sem consulta; está na lista de questões em aberto do README)
- **Data:** 2026-10-17

## Contexto

O briefing citava Hibernate 6 / Spring Security 6 (linha Boot 3.x) e, como fallback, Boot 3.5 +
Spring AI 1.1. Consulta ao Maven Central e às docs oficiais em 17/10/2026:

- Spring Boot **4.1.1** é a linha estável mais recente (suporte OSS até 31/07/2027).
- O Boot **3.5 saiu do suporte OSS em 30/06/2026**.
- A doc do Spring AI diz: *"Spring AI 2.0.x supports Spring Boot 4.0.x and 4.1.x"* (versão 2.0.1).
- Spring Modulith 2.1.1, springdoc 3.1.1 e ShedLock 7.10.1 são compilados contra Spring 7 / Boot
  4.1. O `resilience4j-spring-boot4` 2.4.0 foi compilado contra o Boot 4.0.0.

## Decisão

Usar **Spring Boot 4.1.1 + Spring AI 2.0.1 + Spring Modulith 2.1.1**. Isso implica Hibernate 7.4,
Spring Security 7.1, Jackson 3.1, Flyway 12, Testcontainers 2.0 e JUnit 6.

## Consequências

- Diferenças em relação ao briefing: Hibernate **7** e Security **7**. `@JdbcTypeCode(SqlTypes.JSON)`
  continua igual, e o Hibernate 7.4 já traz `Jackson3JsonFormatMapper`.
- Starters do Boot 4 são modulares (`spring-boot-starter-webmvc`, `-flyway`, `-webmvc-test`…).
- Ecossistema mais novo. Se alguma integração quebrar (ex.: Resilience4j), o plano B é
  configurar o Resilience4j core programaticamente, sem o starter.
