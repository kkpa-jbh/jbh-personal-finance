# jbh-finance

Agent-Name: jbh-finance

## Doc-Pack (module)

- `jbh-finance/docs/FinanceBusinessRules.md`

These add to the project doc pack in `.claude/CLAUDE.md`.

## Responsibility

Personal finance products, their movements, and their monthly balances. It keeps the product
balance and the monthly balances correct when movements are added, removed, uploaded from a file,
or when an official monthly report is registered.

It also imports a bank statement PDF into a SAVINGS or CREDIT_CARD product, in 3 use cases.
Start admits the import (owner and type check, SHA-256 duplicate block, `statement_imports` row
`PROCESSING`) and answers `202`. Run reads the PDF with AI in the background, keeps the main
currency rows, saves them through the file upload use case and marks the import `DONE` in one
transaction. Find (the poll) returns the status, the summary and the saved movements.

## Sub-modules

- `jbh-finance-domain`: domain entities and value objects. No framework.
- `jbh-finance-application`: use cases, services, ports, DTOs, commands. No framework.
  It asks for transactions through `UnitOfWork` and for the product lock through
  `ProductLifecycleService.lockForUpdate`.
- `jbh-finance-infra`: REST adapters, Request/Response objects, JPA entities and adapters,
  `QuarkusUnitOfWork`, CDI wiring (`ProductUseCasesCDIConfig`, `StatementUseCasesCDIConfig`),
  Liquibase changelog, bank statement import adapters.

## Public interface

- REST endpoints in `jbh-finance-infra` (`*RestAdapter`), routed by `jbh-gateway`.
- Use case interfaces in `jbh-finance-application` (`*UseCase`), wired as CDI beans in infra.

## Imports

- Domain and application may use only the JDK, SLF4J, Lombok and `jbh-commons-lib`.
- Application never imports infra. Infra may import application and domain.
- Infra calls other services only through `jbh-gateway-client`.
- Never read another module's internals or database schema.

## Data

- PostgreSQL, database `jbh_finance`, schema `finance`, datasource and persistence unit `finance`.
- Tables: `products`, `movements`, `monthly_balances`, `categories`, `statement_imports`.
- Migrations: `jbh-finance-infra/src/main/resources/db/finance-db-master.xml` (Liquibase,
  run at start). The schema itself is created outside Liquibase.

## Concurrency rule

The app runs as more than one instance. Every flow that changes a product's balances opens one
transaction and calls `lockForUpdate(productId)` first. Background adjust tasks follow the same rule.

## DTO conventions

`*JPAEntity` (infra) to `*DTO` (application) to `*Response` (infra). Requests and Responses live
only in infra. See `.claude/standards/naming.md`.

## Testing notes

- Unit tests live in `jbh-finance-application` (in-memory fakes in `testfixtures/fakes`,
  `UnitOfWorkTest`, Mockito). Wait for background work with `awaitBackgroundTasks()`, never a sleep.
- `jbh-finance-infra` has opt-in `@QuarkusTest` classes on a real PostgreSQL (tag `database`),
  including the statement import tests. They need Docker. Run them with
  `-Djbh.test.excludedGroups=none`. All other infra tests are plain JUnit.
- In those tests `FakeExtractStatementMovementsUseCase` replaces the AI reader and classifier, so no
  test calls Azure, OpenRouter or Jev. Wait for a background import by polling the GET with a time
  limit, never a fixed sleep.
