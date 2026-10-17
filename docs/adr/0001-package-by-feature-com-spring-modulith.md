# ADR 0001 — Package by feature com Spring Modulith

- **Status:** aceito
- **Data:** 2026-10-17

## Contexto

O MVP tem domínios bem distintos (cadastro, agenda, telefonia, IA, áudio, leitura do tutor) que
evoluem em ritmos diferentes. Organizar por camada técnica (`controllers/`, `services/`…) espalha
cada funcionalidade por vários pacotes e não impede acoplamento acidental.

## Decisão

- Cada subpacote direto de `br.com.cuida` é um **módulo de funcionalidade** autocontido
  (controller, DTOs, serviço, entidade, repositório, eventos).
- Visibilidade **package-private** por padrão; só é `public` a API do módulo (fachada, DTOs de
  leitura, eventos).
- Módulos não acessam entidades/repositórios uns dos outros: conversam por fachada pública ou
  **eventos de domínio**.
- `shared` é um módulo `OPEN` apenas com infraestrutura transversal e **não depende de nenhuma
  feature**.
- O **Spring Modulith** verifica a estrutura no build (`ModularityTests`): ciclos, acesso a
  classes internas e dependências fora do `allowedDependencies` de cada `package-info.java`
  quebram o build. Uma regra ArchUnit proíbe pacotes por camada técnica.
- A documentação dos módulos (PlantUML + canvases) é gerada em `docs/modules/`.

## Consequências

- Fronteiras explícitas e verificadas automaticamente; extrair um módulo para um serviço próprio
  no futuro fica mais barato.
- O módulo `call` fica com ~20 classes planas (acima da diretriz de ~12) porque agendador,
  despachante e sweeper precisam do acesso package-private à entidade `Call`; só os adaptadores
  ficam em subpacotes (`call.vapi`, `call.mock`).
- Testes que precisam de dados de outro módulo usam SQL (`TestFixtures`), não entidades alheias.
