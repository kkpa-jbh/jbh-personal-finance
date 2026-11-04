# Sync proposal — finance-statement-import

> Staged by `kkpa-context-curate from-spec`. This is a **draft** of KB edits derived from one
> approved OpenSpec capability spec. Review/edit the blocks below, then run
> `/kkpa-context-curate apply-sync` to write them into the real KB. Nothing here touches the
> canonical KB until applied. This file is self-contained — it embeds the proposed content, so it
> stays valid even after the OpenSpec change folder is archived/moved.

Target guide: `use-case/jbh-finance/extract-statement-movements.md`
Source spec:  `openspec/specs/finance-statement-import/spec.md`
Generated:    `2026-11-01`
Status: APPLIED 2026-11-01

**Not covered by this proposal (spec.md has no file paths):** the guide's `## Entry point`,
`## Flow` and `## Database` sections still describe the removed `POST /jbh-api/finance/statements/extract`.
They need a `/kkpa-context-curate use-case POST /jbh-api/finance/products/{productId}/statement-imports`
run (and one for the poll GET), which carries the trace and the human gate.

---

## [guide] ## Input / output — REPLACE

- **Upload (input):** `POST /jbh-api/finance/products/{productId}/statement-imports`,
  `multipart/form-data` with `file` (the PDF) and `password` (only for a locked PDF), plus the JWT.
  The product must belong to the user and be `SAVINGS` or `CREDIT_CARD`.
- **Upload (output):** `202` with `importId` and `status` `PROCESSING`; the work runs in the
  background. `409` `STATEMENT_ALREADY_IMPORTED` for the same PDF in the same product while an
  import is `PROCESSING` or `DONE`. `422` `STATEMENT_PRODUCT_TYPE_NOT_ALLOWED` or `PRODUCT_NOT_FOUND`.
- **Poll:** `GET /jbh-api/finance/products/{productId}/statement-imports/{importId}` → status
  (`PROCESSING`, `DONE`, `FAILED`), error code, `summary` (`mainCurrency`, `savedCount`,
  `notSavedMovements` with reason `OTHER_CURRENCY` or `FUTURE_DATE`, totals checks), and the saved
  movements sorted by date ascending: date, description, positive amount, direction, category label
  per language, confidence, needs-review. `422` `STATEMENT_IMPORT_NOT_FOUND` for an import of another
  product or user.
- **The old endpoint is gone:** `POST /jbh-api/finance/statements/extract` answers `404`.

## [guide] ## Conventions & gotchas — APPEND

- **Superseded (2026-11-01):** the "Async (temporary)" notes above no longer hold. The result is now
  saved to the product and returned by the poll GET; the old endpoint answers `404`.
  _Source: spec finance-statement-import — Requirement: A statement import is started for one product._
- **One PDF once per product.** The SHA-256 of the uploaded bytes blocks a second upload of the same
  PDF in the same product (`409`) while the first is `PROCESSING` or `DONE`. A `FAILED` import does not
  block a retry, and the same PDF may go into another product. Two parallel uploads of the same PDF,
  on any instances: exactly one `202`, one `409`.
  _Source: spec finance-statement-import — Requirement: The same PDF cannot be imported twice into one product._
- **Save and DONE in one transaction.** Movements, the product balance, the monthly balances and the
  `DONE` status commit together, so a poll never sees `DONE` with missing movements. CREDIT → deposit,
  DEBIT → withdrawal, applied in date order. The insufficient-funds / credit-limit check does not run
  for a statement import (the bank statement is the truth).
  _Source: spec finance-statement-import — Requirement: Movements are saved to the product._
- **Unknown rule.** No valid Jev answer, or confidence under `min-confidence` (0.7) → the fallback
  category: *Other income* for CREDIT, *Unknown expense* for DEBIT, and "needs review". Confidence
  exactly 0.7 keeps Jev's category. A low-confidence fallback keeps Jev's confidence; no valid answer
  gives 0.
  _Source: spec finance-statement-import — Requirement: Unknown category rule._
- **Main currency only.** The main currency is the one with the most rows; on a tie, the first balance
  section's currency. Rows in other currencies are not saved and come back as `OTHER_CURRENCY`.
  _Source: spec finance-statement-import — Requirement: Only the main currency is saved._
- **Future-dated rows are skipped, not fatal.** A row dated after today is returned as `FUTURE_DATE`
  and the rest is saved; a row dated today is saved. If nothing is left, the import is `DONE` with
  `savedCount` 0. A row in another currency always gets `OTHER_CURRENCY`.
  _Source: spec finance-statement-import — Requirement: Rows dated in the future are skipped._
- **Stale imports.** A `PROCESSING` import older than the configured time (default 10 min) reads as
  `FAILED` (`STATEMENT_IMPORT_TIMED_OUT`). A new upload of the same PDF first marks the stale row
  `FAILED`. If the old job finishes later, its transaction rolls back and the status stays `FAILED`.
  _Source: spec finance-statement-import — Requirement: A stuck import counts as failed._
- **Poll is instance-independent.** The owner check uses the product in the path and the signed-in
  user; the answer comes only from the database.
  _Source: spec finance-statement-import — Requirement: The client polls the result._

## [guide] ## Related use cases — REPLACE

- The poll `GET /jbh-api/finance/products/{productId}/statement-imports/{importId}` reads the result of
  this import. Not curated as its own use-case file yet.
- Saving reuses the bulk movement upload of the product (one transaction, product row lock). Not
  curated as its own use-case file yet.

## [index] ## Glossary & routing — entities — ADD ROWS

| statement import | `ExtractStatementMovementsUseCase` | use case | `use-case/jbh-finance/extract-statement-movements.md` |
| save statement movements | `ExtractStatementMovementsUseCase` | use case | `use-case/jbh-finance/extract-statement-movements.md` |
