# ADR 0001 — Package by feature com Spring Modulith

- **Status:** aceito (revisado em 2026-10-17: estrutura e nomes no padrão do `exemplos/backend`)
- **Data:** 2026-10-17

## Contexto

O MVP tem domínios bem distintos (cadastro, agenda, telefonia, IA, áudio, leitura do tutor) que
evoluem em ritmos diferentes. Organizar por camada técnica (`controllers/`, `services/`…) espalha
cada funcionalidade por vários pacotes e não impede acoplamento acidental.

A primeira versão deixava tudo plano em cada pasta (entidade, DTOs, `View`, `Request`, `Response`,
serviços), o que ficou confuso de navegar. A equipe já usa outro projeto como referência de
estrutura (`exemplos/backend`, Tempo Claro), e o Cuida passa a seguir o mesmo formato.

## Decisão

- Cada subpacote direto de `br.com.cuida` é um **módulo de funcionalidade**, com **nome em
  português** (`idoso`, `agenda`, `questionario`, `ligacao`, `webhook`, `analise`, `gravacao`,
  `calendario`, `auditoria`, `autenticacao`).
- Dentro de cada módulo, como no exemplo:
  - na raiz: `XxxController` (só delega), `XxxService`, `XxxRepository`, `XxxJob` e classes de
    lógica com nome descritivo (`PlanejadorDeLigacoes`, `DisparadorDeLigacoes`,
    `NormalizadorDeResumo`…);
  - em `model/`: entidades, enums, eventos e DTOs com sufixo `RequestDTO`, `ResponseDTO` ou `DTO`;
  - adaptadores externos em subpastas (`vapi/`, `mock/`, `openai/`).
- O transversal fica em `infra/` (`exception`, `jwt`, `security`, `doc`, `saude`, `util`,
  `persistencia`, `agendamento`, `eventos`), um módulo `OPEN` que **não depende de nenhuma
  funcionalidade**.
- Erros de negócio usam as exceções de `infra/exception` (`RecursoNaoEncontradoException`,
  `RegraDeNegocioException`, `ConflitoException`, `ValidacaoException`…), convertidas pelo
  `GlobalExceptionHandler`.
- Módulos conversam só pelos services públicos, pelos tipos de `model/` (interface nomeada
  `model` do Modulith, declarada em `model/package-info.java`) ou por **eventos de domínio**.
  Repositórios e classes de lógica continuam internos.
- O **Spring Modulith** verifica a estrutura no build (`ModularityTests`): ciclos, acesso a
  classes internas e dependências fora do `allowedDependencies` quebram o build. Uma regra ArchUnit
  proíbe pacotes por camada técnica.
- Rotas, JSON e colunas do banco continuam em inglês (contrato, ADR 0007); nomes de métodos e
  campos internos também ficaram como estavam.

## Consequências

- Cada pasta tem poucos arquivos na raiz e o mesmo formato em todos os módulos.
- Como `model/` é outro pacote Java, entidades e DTOs são `public`; o encapsulamento entre módulos
  passa a depender só do Modulith (que bloqueia o acesso a qualquer coisa fora de `model/` e dos
  services públicos).
- O módulo `ligacao` ainda tem ~18 classes na raiz (agendador, disparador, retentativas,
  varredor…), porque cada etapa do ciclo é uma classe pequena e testável.
- Testes que precisam de dados de outro módulo usam SQL (`TestFixtures`), não entidades alheias.
