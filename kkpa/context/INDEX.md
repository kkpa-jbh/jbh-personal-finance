# Knowledge Base — INDEX

> Entry point for the `kkpa-context-fetch` skill. Resolve a UI/business term to its internal
> name and KB guide before touching any concept. One row per term **and per alias**.
> Paths are relative to `kkpa/context/`. The Module column is for browsing only.

## Glossary & routing — entities

| Term (UI / business) | Internal name | Type | KB path |
|---|---|---|---|
| bank statement import | `ExtractStatementMovementsUseCase` | use case | `use-case/jbh-finance/extract-statement-movements.md` |
| import statement | `ExtractStatementMovementsUseCase` | use case | `use-case/jbh-finance/extract-statement-movements.md` |
| extracto bancario | `ExtractStatementMovementsUseCase` | use case | `use-case/jbh-finance/extract-statement-movements.md` |
| upload monthly bank report | `ExtractStatementMovementsUseCase` | use case | `use-case/jbh-finance/extract-statement-movements.md` |

## Input ports — pages & endpoints

| Page / endpoint | Route or URI | Module | KB path |
|---|---|---|---|
| Import statement page | `/transactions/import-statement` | `jbh-finance` | `input-port/jbh-finance/import-statement.md` |

## Use cases

| Use case | Entry point | Module | KB path |
|---|---|---|---|
| Extract statement movements (bank statement import) | `POST /jbh-api/finance/statements/extract` | `jbh-finance` | `use-case/jbh-finance/extract-statement-movements.md` |

## Workflows

| Workflow | KB path |
|---|---|
| monthly balance sync | `workflows/monthly-balance-sync.md` |
| monthly balance | `workflows/monthly-balance-sync.md` |
| product row lock | `workflows/monthly-balance-sync.md` |

## Architecture topics

| Topic | KB path |
|---|---|
