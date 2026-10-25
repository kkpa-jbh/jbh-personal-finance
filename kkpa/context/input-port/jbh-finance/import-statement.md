# Import statement page

> **Adapter side:** what the outside world calls, and where it forwards to. **No backend flow
> here** — that lives in the linked use-case file.
>
> "Input port" in this KB means the *driving adapter* — the page, route, or endpoint that
> triggers a use case. (Strict hexagonal reserves "port" for the interface the core exposes;
> this KB deliberately uses the looser sense.)
>
> All KB links below are relative to `kkpa/context/`. File paths start with the repo folder
> (`jbh-app/`, `jbh-gateway/`), which are siblings in the `kkpa-jbh` folder.

## Page

- **Route / URI:** `/transactions/import-statement`
- **Description:** The user uploads a bank statement PDF (with its password when it is locked)
  and sees the movements with a suggested category each, plus a totals warning. Nothing is saved.
- **Module:** `jbh-finance`

## Front-end component map

| File | Role |
|---|---|
| `jbh-app/src/app/app.routes.ts` | Route `transactions/import-statement` → `ImportStatementPage` |
| `jbh-app/src/app/ui/products/pages/import-statement/import-statement.page.ts` | Page logic: pick the file, password, call the use case, show results |
| `jbh-app/src/app/ui/products/pages/import-statement/import-statement.page.html` | Page template |
| `jbh-app/src/app/ui/products/pages/import-statement/import-statement.page.scss` | Page styles |
| `jbh-app/src/app/core/domain/use-cases/statement.use-case.ts` | Front-end use case the page calls |
| `jbh-app/src/app/core/domain/repositories/statement.repository.ts` | Repository contract |
| `jbh-app/src/app/core/infrastructure/repositories/statement.repository.impl.ts` | HTTP call: `FormData` (`file`, `password`) to `/finance/statements/extract` |
| `jbh-app/src/app/core/domain/features/statements/` | Response types (`api/responses/`), models, `statement.types.ts`, `index.ts` |

## Gateway

- **Gateway route:** `/jbh-api/finance/**` → `lb://jbh-personal-finance` (Consul name).
- **Auth / transforms:** the gateway checks the JWT (header or `JBH_TOKEN` cookie) and applies
  the rate limit. In production Caddy sends `/jbh-api/*` to the gateway.
- **Files:** `jbh-gateway/src/main/resources/application.yml`

## Endpoints

| Method + path | Purpose | Use case |
|---|---|---|
| `POST /jbh-api/finance/statements/extract` | Check and unlock the PDF, then read and classify its movements in the background; answers `202` at once (temporary, since 2026-10-02) | `use-case/jbh-finance/extract-statement-movements.md` |

## Conventions & gotchas

- **The page still expects the old `200` result.** Since 2026-10-02 the endpoint answers `202`
  `{"status":"PROCESSING"}`, so `StatementClassification.fromApiResponse` fails and the page shows
  its generic error toast. Accepted while the OCR and LLM providers are tested. When the result
  comes back, only `statement.repository.impl.ts` should need to change (POST, then poll), plus the
  response types (`totalsChecks` per currency, `currency` per movement).
  _Source: user decision, 2026-10-02._
- `EXTRACT_TIMEOUT_MS` (150 s) in `statement.repository.impl.ts` was the limit that made the
  backend go async.
