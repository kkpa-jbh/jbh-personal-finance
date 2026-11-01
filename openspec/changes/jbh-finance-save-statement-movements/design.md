# Design: jbh-finance-save-statement-movements

Source ticket: JBH-73 - https://linear.app/kkpa/issue/JBH-73/save-imported-statement-movements-to-a-product

Module: `jbh-finance` (domain, application, infra). Backend only.

## Context

The import runs today as: REST adapter unlocks the PDF, `StatementImportDispatcher` starts a
virtual thread, `StatementImportRunner` (`@ActivateRequestContext`) calls
`ExtractStatementMovementsUseCase`, and the result is only logged. The use case reads the PDF
(OCR + LLM), checks totals per currency, loads categories, and asks Jev for one category per row.

`AddMovementsUploadedFileUseCase` already saves a list of movements in one transaction: it takes
the product row lock first, syncs the product balance, saves the movements, and saves the monthly
balances (the adjust step runs in the background under the same lock rule).

The app runs as 2 or more instances. All shared state lives in PostgreSQL.

## Decisions

Decisions D1 to D15 and D25 are the user's answers (made_by: user). They are binding.
D16 to D24 are design choices of this document (made_by: agent) that fill gaps.

### D1. Async + save + poll (user)

Upload answers `202` with an import id. A background job saves the movements. The app polls a GET
that returns status, summary and the saved movements sorted by date.

### D2. Unknown rule (user)

A movement gets the Unknown category when Jev has no valid answer OR confidence < `min-confidence`
(0.7). Money in (CREDIT) gets the **Other income** category (`CategoryService.findIncomeOther`).
Money out (DEBIT) gets the **Unknown expense** category (`findExpenseUnknown`). Today money in
wrongly gets the expense Unknown and low confidence keeps Jev's pick. Both change. Code:
`ExtractStatementMovementsInputPort.categorize`, `StatementCategoryCatalog`.

### D3. Currency (user)

Products have no currency. Save only rows in the statement's main currency. The other rows are
returned in the poll answer marked "not saved". Main currency = the currency with the most rows;
on a tie, the currency of the first balance section.

### D4. Duplicates (user)

Block the same PDF twice on one product: SHA-256 of the uploaded PDF bytes, unique constraint per
product, answer `409`. FAILED imports do not block a retry (partial unique index excluding FAILED).

### D5. Reuse `AddMovementsUploadedFileUseCase` (user)

Reuse `AddMovementsUploadedFileUseCase` (`AddMovementsUploadedFileInputPort`: one transaction,
product row lock first, product balance sync, monthly balance sync). Extend
`AddMovementUploadedFileCommand` with optional category, description, statement import id and
review data (confidence, needs-review). When the category is absent, keep today's behavior (Other
income / Unknown expense). Movement type from direction: CREDIT -> DEPOSIT, DEBIT -> WITHDRAWAL.
`balanceSnapshot` is null for statement rows.

### D6. Product types allowed (user)

SAVINGS and CREDIT_CARD only. Others -> `422` with a new error `STATEMENT_PRODUCT_TYPE_NOT_ALLOWED`.
The product must belong to the user (existing `findOrThrowByUserAndProductId`).

### D7. Database: one new table (user)

`finance.statement_imports`: id, user id, product id (FK), `pdf_sha256`, status (`PROCESSING` /
`DONE` / `FAILED`), error code, `summary` jsonb, started/finished timestamps. Partial unique index
on (product_id, pdf_sha256) WHERE status <> 'FAILED'. Key choice: see "Database design".

### D8. Link movements -> import: new FK column (user)

`finance.movements.statement_import_id UUID NULL` -> `statement_imports`, with a partial index
(statement_import_id, movement_date) WHERE statement_import_id IS NOT NULL. The poll reads
movements by this column, sorted by date.

### D9. Extra data (user)

`statement_imports.summary` jsonb (written once at the end) holds the "not saved" rows, the totals
checks per currency, and counts (saved, skipped, needs review). The per-movement confidence and
"needs review" flag go into each movement's existing `metadata` jsonb (new `MovementMetadataKey`
values).

### D10. Stuck jobs: stale timeout (user)

A `PROCESSING` row older than a config value (default 10 min) counts as FAILED. The poll reports it
as FAILED. A new upload of the same PDF first marks it FAILED with ONE atomic UPDATE
(`... WHERE status='PROCESSING' AND started_at < now() - interval`), then inserts. No DB queue, no
scheduler, the PDF is never stored.

### D11. Funds check: skip it for statement imports (user)

The bank already accepted the movements, so the statement is the truth. Rows are applied in date
order. The insufficient-funds / credit-limit check (`ProductDomain.validateInsufficientNetFlow`,
`SavingsProductCreationValidator`, `CreditCardProductCreationValidator`) is off ONLY for statement
imports, through the movement options value object (`ProcessMovementOptionsVO`), never a new
boolean method parameter (project rule).

### D12. API shape, product in the path (user)

`POST /jbh-api/finance/products/{productId}/statement-imports` (multipart: `file`, optional
`password`) -> `202 {importId, status}`.
`GET /jbh-api/finance/products/{productId}/statement-imports/{importId}` -> status, error code,
summary, and the saved movements sorted by date, each with the category label (display name per
language), amount, direction, description, confidence, needs-review. Owner check from the path and
the JWT user. Follows `.claude/architecture/rest-organization.md` and `FinanceApiRoutes`.

### D13. Remove the old endpoint (user)

Remove `POST /jbh-api/finance/statements/extract` (`StatementRestAdapter.extractMovements` and its
now-unused response classes). Keep `ExtractStatementMovementsUseCase`: the new run use case calls
it. Keep the dev-only LLM comparison (`AI/llmcompare/`, `JevReport`) as it is.

### D14. SOLID: SRP + DIP only (user)

No OCP abstractions, no ISP split. DIP: the application layer reaches the new table only through a
`StatementImportRepository` output port.

### D15. SRP split (user confirmed)

One job per class: `StartStatementImportInputPort`, `RunStatementImportInputPort`,
`FindStatementImportInputPort` (each with its `*UseCase`), `StatementMainCurrencyPolicy`,
`StatementImportRepository`. See "Responsibilities".

### D16. The job finishes in the same transaction as the save (agent)

`RunStatementImportInputPort` wraps two calls in ONE `unitOfWork.executeInTransaction`:
`uploadMovementsFromFile(...)` and `StatementImportRepository.markDone(...)`.
`QuarkusUnitOfWork` is `@Transactional` (REQUIRED), so the inner use case joins it. The product row
lock stays the first statement (it is the first thing `uploadMovementsFromFile` does).
Why: a poll never sees `DONE` with missing movements, and a crash between the two writes cannot
leave saved movements under a `PROCESSING` row that later becomes "stale" and lets the user import
the same PDF again (duplicate movements). The long AI work (OCR, LLM, Jev) happens BEFORE this
transaction, so no DB connection is held during it.
If `markDone` updates 0 rows (the row was already marked `FAILED` as stale), the use case throws
`BusinessException(STATEMENT_IMPORT_TIMED_OUT)` inside the transaction. Everything rolls back.

### D17. Insert and stale-free are single atomic statements (agent)

`insertProcessing` is `INSERT ... ON CONFLICT (product_id, pdf_sha256) WHERE status <> 'FAILED' DO
NOTHING` and returns whether a row was inserted. No exception, so a duplicate does not poison the
transaction. `markStaleAsFailed` is one `UPDATE ... WHERE product_id=? AND pdf_sha256=? AND
status='PROCESSING' AND started_at < now() - make_interval(secs => ?)`. Both use the database
clock. Adapter methods are `@Transactional` (REQUIRED).

### D18. Hash the original upload (agent)

The SHA-256 is computed on the bytes the client uploaded, before the PDF is unlocked. PDFBox
unlock rewrites the file, and the same file with a different password text must give the same hash.
Value object: `PdfSha256` (lower-case hex, 64 characters).

### D19. HTTP status mapping (agent)

`FinanceGlobalExceptionHandler` maps every `BusinessException` to `422` today. It gets one
override: `STATEMENT_ALREADY_IMPORTED` -> `409`. Every other new error stays `422`
(`STATEMENT_PRODUCT_TYPE_NOT_ALLOWED`, `STATEMENT_IMPORT_NOT_FOUND`), like `PRODUCT_NOT_FOUND`.

### D20. Main-currency tie rule, exact reading (agent)

Count rows per currency. Take the highest count. If several currencies tie, pick the one whose
balance section comes first in `StatementClassificationDTO.balances` (the PDF order). If none of
the tied currencies has a balance section, pick the one that appears first in the movements.
For this the classification result gets the balances list (see D22).

### D21. Fallback keeps the classifier confidence (agent)

When a valid answer has confidence under the minimum, the category becomes the fallback but the
confidence stays what Jev said (for example `0.5`), so the user sees how weak the guess was. With
no valid answer the confidence is `0`. In both cases `needsReview` is `true`. A valid answer that
is itself the fallback category is also marked `needsReview` (same as today).

### D22. Result DTO carries the chosen category and the balances (agent)

`CategorizedMovementDTO` gets `CategoryDTO category` (the chosen one, so the run use case can build
the command without a lookup) and `StatementClassificationDTO` gets `List<StatementBalanceDTO>
balances`. The existing components stay. Only `ExtractStatementMovementsInputPort` builds them.

### D23. One batch command carries the options (agent)

`uploadMovementsFromFile(userId, productId, commands)` stays. A new overload takes
`UploadMovementsBatchCommand(List<AddMovementUploadedFileCommand> movements,
ProcessMovementOptionsVO options)`: three parameters, no boolean parameter. The old method calls
the new one with `UploadMovementsBatchCommand.forFile(...)`. `ProductLifecycleService` gets
`syncByUploadedMovements(domain, movements, options)`; the 2-argument method stays and delegates.

### D24. The run use case returns the classification for the dev report (agent)

`RunStatementImportUseCase.run` returns `Optional<StatementClassificationDTO>` (empty when the
import failed). `StatementImportRunner` passes it to `JevReport.write` when present, so the dev
comparison report keeps working unchanged.

### D25. Future-dated rows are skipped, not fatal (user)

A row whose date is after today (an LLM reading error) is NOT saved. It goes into the "not saved"
list of the summary with reason `FUTURE_DATE`, next to the other-currency rows (reason
`OTHER_CURRENCY`). The reasons are one closed set: `OTHER_CURRENCY`, `FUTURE_DATE`. The rest of
the import is saved. The domain check and the `movement_date <= CURRENT_DATE` table check stay as
they are; the run use case filters before the save. "Today" comes from the injected `Clock`
(`RunStatementImportInputPort` gets a `Clock`, like `FindStatementImportInputPort`).
Agent detail: a row that is in another currency AND in the future gets `OTHER_CURRENCY` (the
currency check runs first). The main currency is still picked from all extracted rows (D3, D20).
If no row is left to save, the upload is not called (it refuses an empty list) and the import ends
`DONE` with `savedCount` 0.

## Responsibilities

New classes (one job each):

| Class | Layer | One responsibility |
|---|---|---|
| `StartStatementImportInputPort` (+ `StartStatementImportUseCase`) | application | Admit an import: owner check, product type check, free a stale row, insert the `PROCESSING` row (duplicate -> `STATEMENT_ALREADY_IMPORTED`). |
| `RunStatementImportInputPort` (+ `RunStatementImportUseCase`) | application | Run one import: extract, pick main currency, map rows to commands, save with the existing upload use case, mark `DONE` or `FAILED`. |
| `FindStatementImportInputPort` (+ `FindStatementImportUseCase`) | application | Build the poll answer: owner check, effective status, saved movements sorted by date. |
| `StatementMainCurrencyPolicy` | application (services) | Pick the main currency (D3, D20). |
| `PdfSha256` | application (vo) | Hold and compute the SHA-256 of PDF bytes. |
| `StatementImportRepository` | application (ports/output) | Output port for `statement_imports` rows. |
| `UploadMovementsBatchCommand` | application (movement commands) | Carry the movements and the processing options of one upload. |
| `StatementImportCommands` (`StartStatementImportCommand`, `RunStatementImportCommand`, `FindStatementImportCommand`) | application | Input of each use case. |
| `StatementImportStatus`, `StatementImportDTO`, `NewStatementImportDTO`, `StatementImportSummaryDTO`, `SkippedStatementMovementDTO`, `StatementImportResultDTO`, `StatementImportMovementDTO`, `StatementImportSettings` | application (dto) | Data carriers only. |
| `MovementReviewVO` | domain | Confidence and needs-review of one movement. |
| `StatementImportRestAdapter` | infra | HTTP only: POST and GET, request and response mapping. |
| `StatementImportJPAEntity`, `StatementImportJPARepository`, `StatementImportRepositoryAdapter` | infra | Persist and read `statement_imports`; implement the port. |
| Responses: `StatementImportAcceptedResponse`, `StatementImportResponse`, `StatementImportSummaryResponse`, `StatementImportMovementResponse`, `SkippedStatementMovementResponse`, `TotalsCheckResponse` (kept, moved use) | infra | API contract. |

Existing classes that get a new job (each change is small and named):

| Class | New job |
|---|---|
| `ExtractStatementMovementsInputPort` / `StatementCategoryCatalog` | The new Unknown rule per direction (D2, D21, D22). |
| `AddMovementUploadedFileCommand` | Carry optional category, description, import id, review data. |
| `AddMovementsUploadedFileInputPort` | Use those fields; pass the options to the product sync. `mapCommandToDomain` is split first (see Complexity). |
| `ProcessMovementOptionsVO` / `ProductDomain.syncBalancesByMovement` | The "skip funds check" option (D11). |
| `ProductType` | `allowsStatementImport()` (SAVINGS, CREDIT_CARD). |
| `MovementMetadata` / `MovementMetadataKey` | Store confidence and needs-review. |
| `MovementDomain` / `MovementDTO` / `MovementMapper` / `MovementJPAEntity` | Carry the nullable `statementImportId`. |
| `MovementQueryRepository` (+ adapter, JPA repo) | `findByStatementImportId(UUID)`. |
| `StatementImportRunner` / `StatementImportDispatcher` | Call the run use case with the import id and the user (D24). |
| `FinanceGlobalExceptionHandler` | `409` for `STATEMENT_ALREADY_IMPORTED` (D19). |
| `StatementUseCasesCDIConfig` | Produce the three new use cases. |
| `StatementImportConfig` | `import.stale-after` setting. |

Removed: `StatementRestAdapter`, `StatementClassificationResponse`, `CategorizedMovementResponse`
(`TotalsCheckResponse` is kept and reused by the poll answer).

## Flow

POST (synchronous part, inside the REST call):

1. `StatementImportRestAdapter.start`: user id from the JWT, file checks (present, PDF, size), read
   the bytes, `PdfSha256.of(bytes)`, unlock the PDF (`PdfStatementUnlocker`; password errors are
   still `422` and nothing is stored).
2. `StartStatementImportUseCase.start(StartStatementImportCommand)`: `findOrThrowByUserAndProductId`;
   type check; `markStaleAsFailed`; `insertProcessing`; `false` -> `409`. Returns the import id.
3. `StatementImportDispatcher.dispatch(RunStatementImportCommand)`; answer `202 {importId,
   status:PROCESSING}`.

Background (virtual thread, `StatementImportRunner`, `@ActivateRequestContext`):

4. `RunStatementImportUseCase.run`: `extract` (no transaction) -> main currency -> split rows ->
   one transaction { `uploadMovementsFromFile(userId, productId, batch(forStatement))` ;
   `markDone(importId, summary)` }. On `BusinessException` -> `markFailed(importId, enum name)`.
   On `RuntimeException` -> `markFailed(importId, "STATEMENT_IMPORT_FAILED")`. Never rethrows.

GET:

5. `FindStatementImportUseCase.find`: owner check; `findByIdAndProduct` (unknown id or other
   product -> `STATEMENT_IMPORT_NOT_FOUND`); effective status; if `DONE`, movements via
   `MovementQueryRepository.findByStatementImportId`, mapped and sorted by (date, createdAt).

Mapping of a saved row (CREDIT -> DEPOSIT, DEBIT -> WITHDRAWAL):

| Statement row | `AddMovementUploadedFileCommand` |
|---|---|
| `date` | `entryDate` |
| CREDIT / DEBIT | `movementType` DEPOSIT / WITHDRAWAL |
| `amount` (positive) | `totalAmount` = `+amount` for CREDIT, `-amount` for DEBIT (the domain wants withdrawals negative) |
| (none) | `balanceSnapshot` = `null` |
| chosen category | `category` |
| `description` | `description` |
| import id | `statementImportId` |
| confidence, lowConfidence | `review` = `MovementReviewVO(confidence, needsReview)` |

Rows are sorted by date ascending (stable, so the PDF order stays inside one day).
Before the save, the run use case splits the rows (D25): a row in another currency is skipped with
reason `OTHER_CURRENCY`; a row dated after `LocalDate.now(clock)` is skipped with reason
`FUTURE_DATE`; the rest is saved. If nothing is left to save, the upload is not called and the
import ends `DONE` with `savedCount` 0.

## API contract

`POST /jbh-api/finance/products/{productId}/statement-imports`, `multipart/form-data`, fields
`file` (required), `password` (optional). `Authorization: Bearer <jwt>`.

| Case | Status | Body |
|---|---|---|
| Accepted | 202 | `{"importId":"<uuid>","status":"PROCESSING"}` |
| No / bad / big file, wrong password | 422 | existing `STATEMENT_*` errors |
| Not the user's product | 422 | `PRODUCT_NOT_FOUND` |
| Product type not allowed | 422 | `STATEMENT_PRODUCT_TYPE_NOT_ALLOWED` |
| Same PDF `PROCESSING` or `DONE` | 409 | `STATEMENT_ALREADY_IMPORTED` |
| No valid user | 401 | existing |

`GET /jbh-api/finance/products/{productId}/statement-imports/{importId}` -> `200`:

```json
{
  "importId": "uuid",
  "status": "DONE",
  "errorCode": null,
  "startedAt": "2026-11-01T10:00:00Z",
  "finishedAt": "2026-11-01T10:01:20Z",
  "summary": {
    "mainCurrency": "COP",
    "savedCount": 3, "skippedCount": 1, "needsReviewCount": 1,
    "totalsChecks": [{"currency":"COP","status":"MATCH","expectedChange":930.00,"movementsChange":930.00}],
    "notSavedMovements": [{"date":"2026-09-02","description":"AMAZON","amount":5.00,"currency":"USD",
      "direction":"DEBIT","reason":"OTHER_CURRENCY","categoryAlias":"EXP_SHOPPING",
      "categoryDisplayName":{"en":"Shopping","es":"Compras"},"confidence":0.9,"needsReview":false}]
    // reason is one of: OTHER_CURRENCY, FUTURE_DATE
  },
  "movements": [
    {"movementId":"uuid","date":"2026-09-01","description":"NOMINA","amount":1000.00,"direction":"CREDIT",
     "category":{"alias":"INC_SALARY","displayName":{"en":"Salary","es":"Salario"}},
     "confidence":0.95,"needsReview":false}
  ]
}
```

`summary` is `null` unless `DONE`. `movements` is `[]` unless `DONE`. `errorCode` is the enum name
of the failure (`STATEMENT_READER_FAILED`, `STATEMENT_NO_MOVEMENTS_FOUND`,
`STATEMENT_IMPORT_TIMED_OUT`, `STATEMENT_IMPORT_FAILED`, a domain error like
`FUTURE_MOVEMENT_DATE`, ...). Error 404 is not used (D19).

The frontend `jbh-app` is a separate change. It will need these exact shapes.

## Database design

### Primary key: surrogate UUID, not natural

`.claude/infrastructure/database.md` prefers a natural key. For `statement_imports` the only
natural candidate is (`product_id`, `pdf_sha256`). It is unique only among non-`FAILED` rows
(D4): a retry after a failure creates a second row for the same pair. So it cannot be the PK
unless a retry reuses and rewrites the failed row, which loses the history and lets an old poll
link show a new run. The poll also needs one opaque value in the URL. So the PK is a surrogate
`id UUID`, generated by the application (the same type `products` and `movements` already use).
The natural pair is enforced by the partial unique index instead.

### Liquibase changeset

File `jbh-finance/jbh-finance-infra/src/main/resources/db/changelog/007_create_statement_imports.sql`
(plain SQL like `001`..`006`; Liquibase runs one file as one changeset, in one transaction on
PostgreSQL). Register it in `db/finance-db-master.xml` after the `006` include.

```sql
-- One row per statement import attempt.
CREATE TABLE finance.statement_imports
(
    id          UUID PRIMARY KEY,
    user_id     UUID                     NOT NULL,
    product_id  UUID                     NOT NULL,
    pdf_sha256  TEXT                     NOT NULL,
    status      TEXT                     NOT NULL,
    error_code  TEXT                     NULL,
    summary     JSONB                    NULL,
    started_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT chk_statement_imports_status
        CHECK (status IN ('PROCESSING', 'DONE', 'FAILED')),
    CONSTRAINT chk_statement_imports_sha256
        CHECK (pdf_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_statement_imports_finished
        CHECK ((status = 'PROCESSING') = (finished_at IS NULL)),
    CONSTRAINT chk_statement_imports_error
        CHECK ((status = 'FAILED') = (error_code IS NOT NULL)),
    CONSTRAINT chk_statement_imports_summary
        CHECK (status <> 'DONE' OR summary IS NOT NULL),

    CONSTRAINT fk_statement_imports_product_id
        FOREIGN KEY (product_id) REFERENCES finance.products (id) ON DELETE CASCADE
);

-- D4: the same PDF cannot be PROCESSING or DONE twice in one product. FAILED rows do not count.
CREATE UNIQUE INDEX uk_statement_imports_product_pdf_active
    ON finance.statement_imports (product_id, pdf_sha256)
    WHERE status <> 'FAILED';

-- Needed by the ON DELETE CASCADE from products (the partial index above skips FAILED rows).
CREATE INDEX idx_statement_imports_product_id ON finance.statement_imports (product_id);

-- D8: link from a movement to the import that created it.
ALTER TABLE finance.movements
    ADD COLUMN statement_import_id UUID NULL
        CONSTRAINT fk_movements_statement_import_id
            REFERENCES finance.statement_imports (id) ON DELETE SET NULL;

CREATE INDEX idx_movements_statement_import_date
    ON finance.movements (statement_import_id, movement_date)
    WHERE statement_import_id IS NOT NULL;

COMMENT ON TABLE finance.statement_imports IS
    'Bank statement import attempts. The PDF is never stored, only its SHA-256.';
```

Notes:

- `status` is `TEXT` + `CHECK` (not a PG enum), like `movement_type` and `type` in `001`: easy to
  extend later with a new Liquibase file.
- The `chk_statement_imports_finished` and `_error` checks make impossible states impossible
  (a `DONE` row without a finish time, a `FAILED` row without a code).
- `ON DELETE CASCADE` on `product_id` matches `movements` and `monthly_balances`. A product delete
  removes its imports and movements.
- `ON DELETE SET NULL` on the movement link: when a product is deleted, `statement_imports` and
  `movements` rows are removed by the same cascade. `SET NULL` is safe whatever order PostgreSQL
  removes them in. Imports are never deleted any other way.
- `ADD COLUMN ... NULL` without a default does not rewrite the `movements` table in PostgreSQL 11+.
  Creating the partial index on a column that is all `NULL` today is instant.
- Rollback (manual, if ever needed): drop `idx_movements_statement_import_date`, drop the column
  `statement_import_id`, drop the table `finance.statement_imports`.
- `summary` as `JSONB` (not a child table): it is written once, read whole, and never filtered or
  joined. Rejected: a child table for the "not saved" rows (extra table, extra joins, no query needs it).
- Rejected: a new `currency` column on `movements` (D3: only the main currency is stored, so there
  is nothing to tell apart).
- Rejected: storing the PDF (privacy; the hash is enough for D4).

### Index plan (against the real queries)

| # | Query (adapter) | Index used | Cost |
|---|---|---|---|
| Q1 | Insert PROCESSING + duplicate check: `INSERT ... ON CONFLICT (product_id, pdf_sha256) WHERE status <> 'FAILED' DO NOTHING` | `uk_statement_imports_product_pdf_active` (conflict arbiter) | one index probe |
| Q2 | Free stale: `UPDATE ... WHERE product_id=? AND pdf_sha256=? AND status='PROCESSING' AND started_at < now() - ?` | the same partial unique index when PostgreSQL proves `status='PROCESSING'` implies `status <> 'FAILED'`; else `idx_statement_imports_product_id`. Either way it reads at most a few rows of one product | one to a few rows |
| Q3 | Poll row: `WHERE id=? AND product_id=?` | primary key | one row |
| Q4 | Finish: `UPDATE ... SET status=... WHERE id=? AND status='PROCESSING'` | primary key | one row |
| Q5 | Poll movements: `WHERE statement_import_id=? ` (+ join `categories` by PK) | `idx_movements_statement_import_date` | one import = tens or hundreds of rows. The sort by date is done by the use case in memory |
| Q6 | Bulk insert of movements (existing) | no new cost: the new column is `NULL` for file uploads and the partial index has no entry for them; for statement rows it adds one small index entry per row | negligible |
| Q7 | Product delete cascade | `idx_statement_imports_product_id`, `idx_movements_statement_import_date` (SET NULL lookup) | rare |

No index on `user_id`: nothing reads by user. No index on `status` or `started_at`: no scan by
status exists (no scheduler, D10). No change to `idx_movements_product_date`, so the existing
movement reads (product screens, monthly balances) are not affected.

## Configuration

`StatementImportConfig` (`@ConfigMapping(prefix = "jbh.statement")`) gets one flat setting,
`Duration staleAfter()`, read from `jbh.statement.stale-after=${STATEMENT_IMPORT_STALE_AFTER:10m}`
in `application.properties`. The CDI config maps it to `StatementImportSettings(Duration
staleAfter)` for the Start and Find use cases. The stale UPDATE (Q2) sends it to the database as
seconds. `jbh-deploy` needs no change (the default is enough).
Existing `jbh.statement.jev.min-confidence` (0.7) is unchanged.

## Concurrency

The app runs as 2 or more instances. Protection is in PostgreSQL only: a unique index, atomic
conditional UPDATEs, and the existing product row lock. No in-JVM lock, cache or queue is used for
correctness. The dispatcher's virtual-thread executor only makes one instance faster.

| # | Task | Risk | Guard (proposal) |
|---|---|---|---|
| C1 | Start import (task 3.x) | Two uploads of the same PDF, same product, on two instances at the same time (check-then-act). | No check-then-act. One `INSERT ... ON CONFLICT DO NOTHING` against the partial unique index. PostgreSQL lets one insert win; the other gets 0 rows and the use case answers `409`. |
| C2 | Run import (task 3.x) | Two different PDFs on the same product at once: both save movements and change the product balance (read-modify-write). | Both pass admission (different hashes). The save runs through `AddMovementsUploadedFileInputPort`, whose transaction takes the product row lock (`lockForUpdate`) as the first statement. The second waits, then reads the balance the first committed. No new guard. |
| C3 | Start import vs Run import (task 3.x) | The stale UPDATE (D10) races with the finishing job's `markDone` / `markFailed`. | Every status change is conditional: `WHERE status='PROCESSING'`. PostgreSQL READ COMMITTED re-checks the condition after waiting for the row lock, so exactly one wins. If the finisher wins, the stale UPDATE changes 0 rows and the insert hits the unique index (`DONE` row) -> `409`. If the stale UPDATE wins, `markDone` changes 0 rows, the use case throws inside the transaction (D16), and the movements, the product balance and the monthly balances roll back. The import stays `FAILED` (`STATEMENT_IMPORT_TIMED_OUT`). The user can upload the PDF again. `markFailed` is conditional too, so it never overwrites a `DONE` row. |
| C4 | Run import (task 3.x) | An adjust task of the monthly balance (background, existing) starts for a transaction that later rolls back (C3). | The adjust step takes the same product lock and reads committed data, so after a rollback it recomputes from the real state. No new guard. Covered by the existing lock rule. |
| C5 | Find import (task 3.x) | A poll lands on an instance that did not run the job. | Nothing is kept in memory. The poll reads the import row and the movements from PostgreSQL. `DONE` and the movements commit in one transaction (D16), so a reader sees both or neither. |
| C6 | Dispatch (task 5.x) | The instance dies after the `202` and before the job ends. | The row stays `PROCESSING` until the stale time. The poll shows `FAILED` after 10 minutes (D10), and a new upload of the same PDF frees it. Before that time a new upload gets `409`. |

Assumption: the transaction isolation is PostgreSQL's default READ COMMITTED (the project default).

## Test contract

Expected values that the tests must hold. Unit tests are offline (no database, no container).
They live in `jbh-finance-application` and `jbh-finance-domain` only. Names follow
`should<Expected>When<Condition>` and the single `*Test` suffix. Use `@Tag("unit")`.
Fakes go in `testfixtures/fakes/`.

### Unit tests

Package roots: `com.jbh.finance.test.application.feature...` (application) and
`com.jbh.finance.domain...` (domain).

**U1 `PdfSha256Test`** (`PdfSha256.of(byte[])`, `.value()`)
- `of("abc".getBytes(UTF_8)).value()` = `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`.
- `of(new byte[0]).value()` = `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
- `of(null)` -> `IllegalArgumentException`.
- Two different byte arrays give two different values; the same bytes give equal objects.

**U2 `StatementMainCurrencyPolicyTest`** (`pick(List<CategorizedMovementDTO>, List<StatementBalanceDTO>)` -> `String`)
- 3 COP + 1 USD, no balances -> `COP`.
- 2 COP + 2 USD, balances `[USD, COP]` -> `USD`.
- 2 COP + 2 USD, balances `[COP, USD]` -> `COP`.
- 2 COP + 2 USD, no balances, USD row first in the list -> `USD` (first seen).
- 1 COP + 1 USD, balances `[EUR, USD]` -> `USD` (first balance among the tied currencies).
- Empty movements -> `IllegalArgumentException`.

**U3 `StatementCategoryCatalogTest`** (extend the existing test; `StatementCategoryCatalog.load`)
Use `CategoryServiceMock` (`findIncomeOther`, `findExpenseUnknown`).
- `fallbackAliasFor(CREDIT)` = alias of `findIncomeOther()`.
- `fallbackAliasFor(DEBIT)` = alias of `findExpenseUnknown()`.
- `optionsFor(CREDIT)` last option alias = the Other income alias; it is not the expense Unknown alias.
- `optionsFor(DEBIT)` last option alias = the expense Unknown alias.
- `isAllowed(CREDIT, <expense unknown alias>)` = `false`; `isAllowed(CREDIT, <other income alias>)` = `true`.
- `categoryOf(alias)` returns the `CategoryDTO` for a known alias and for both fallbacks.

**U4 `ExtractStatementMovementsInputPortTest`** (extend; `extract`; `minConfidence` 0.7)
- DEBIT, answer `EXP_GROCERY` 0.5 -> `categoryAlias` = expense Unknown, `confidence` 0.5, `lowConfidence` true. (Replaces the old expectation in `shouldMarkLowConfidenceWhenConfidenceIsBelowMinimum`, which kept `EXP_GROCERY`.)
- CREDIT, answer `INC_SALARY` 0.4 -> alias = Other income, confidence 0.4, `lowConfidence` true.
- CREDIT, no answer -> alias = Other income, confidence 0, `lowConfidence` true.
- DEBIT, no answer -> alias = expense Unknown, confidence 0, `lowConfidence` true.
- CREDIT, answer = the expense Unknown alias, 0.95 -> not allowed for CREDIT -> Other income, confidence 0, true.
- CREDIT, answer `INC_SALARY` 0.7 -> `INC_SALARY`, `lowConfidence` false.
- DEBIT, answer `EXP_GROCERY` 0.9 -> `EXP_GROCERY`, false; `category.getCategoryId()` = the id of `EXP_GROCERY`.
- Result `balances()` equals the balances the reader returned.
- The existing test `shouldFallBackToUnknownWhenAnswerIsMissingOrFromWrongSource` is updated: its rows must be checked per direction (DEBIT -> expense Unknown, CREDIT -> Other income).

**U5 `StartStatementImportInputPortTest`** (`start(StartStatementImportCommand)` -> `UUID`; Mockito for `ProductLifecycleService` and `StatementImportRepository`; settings `staleAfter` = 10 min)
- SAVINGS product, `insertProcessing` returns `true` -> returns a non-null id equal to the id passed to `insertProcessing` (captor). Order (InOrder): `findOrThrowByUserAndProductId`, `markStaleAsFailed(productId, sha, 10 min)`, `insertProcessing`.
- CREDIT_CARD product -> same result.
- Each of CDT, LOAN, INVESTMENT, REAL_ESTATE_INVESTMENT -> `BusinessException` with `STATEMENT_PRODUCT_TYPE_NOT_ALLOWED`; repository has no interaction.
- `insertProcessing` returns `false` -> `BusinessException` with `STATEMENT_ALREADY_IMPORTED`.
- `findOrThrowByUserAndProductId` throws `BusinessException(PRODUCT_NOT_FOUND)` -> same exception; repository has no interaction.
- `null` command -> `GenericSpecificationException`.

**U6 `RunStatementImportInputPortTest`** (`run(RunStatementImportCommand)` -> `Optional<StatementClassificationDTO>`; Mockito for `ExtractStatementMovementsUseCase` and `AddMovementsUploadedFileUseCase`; fake `InMemoryStatementImportRepository`; `RecordingUnitOfWork`; fixed `Clock` at 2026-11-01T10:00:00Z)
- Input: classification with rows (PDF order) A 2026-09-03 DEBIT 50.00 COP `EXP_GROCERY` 0.9; B 2026-09-01 CREDIT 1000.00 COP `INC_SALARY` 0.95; C 2026-09-02 DEBIT 20.00 COP fallback 0.4 (needs review); D 2026-09-02 DEBIT 5.00 USD. Balances `[COP]`.
  - The batch given to `uploadMovementsFromFile` has 3 commands, in the order B, C, A.
  - B: `movementType` DEPOSIT, `totalAmount` 1000.00. C and A: WITHDRAWAL, `totalAmount` -20.00 and -50.00. All: `balanceSnapshot` null, `statementImportId` = the import id, description and category from the row, `review` = (0.95,false), (0.4,true), (0.9,false).
  - Batch options: `skipFundsCheck` true, operation ADD, not month-reported.
  - The repository row is `DONE` with `summary.mainCurrency` COP, `savedCount` 3, `skippedCount` 1, `needsReviewCount` 1, `notSavedMovements` = [D with reason `OTHER_CURRENCY`], `totalsChecks` as the classification gave.
  - `markDone` ran while `unitOfWork.isInTransaction()` was true, and after the upload call.
  - The result is the classification.
- `extract` throws `BusinessException(STATEMENT_READER_FAILED)` -> row `FAILED` with error code `STATEMENT_READER_FAILED`; `uploadMovementsFromFile` never called; `run` does not throw; result is empty.
- `extract` throws `BusinessException(STATEMENT_NO_MOVEMENTS_FOUND)` -> row `FAILED` with that code.
- `uploadMovementsFromFile` throws `BusinessException(INSUFFICIENT_FUNDS)` (any domain error) -> row `FAILED` with `INSUFFICIENT_FUNDS`; `markDone` never called.
- Future date (D25), `Clock` fixed at 2026-11-01: rows B 2026-09-01 CREDIT 1000.00 COP, E 2026-11-02 DEBIT 10.00 COP, F 2026-11-01 DEBIT 7.00 COP (today is allowed). The batch has 2 commands (B, F). Row `DONE` with `savedCount` 2, `skippedCount` 1, `notSavedMovements` = [E with reason `FUTURE_DATE`]. The upload is not rejected, the import is not `FAILED`.
- Future date and other currency (D25): a USD row dated 2026-11-02 -> reason `OTHER_CURRENCY` (not `FUTURE_DATE`).
- Every COP row is in the future (only future rows in the main currency) -> `uploadMovementsFromFile` never called; row `DONE` with `savedCount` 0 and all rows in `notSavedMovements` with reason `FUTURE_DATE`.
- `extract` throws `IllegalStateException` -> row `FAILED` with `STATEMENT_IMPORT_FAILED`; `run` does not throw.
- The fake repository returns `false` from `markDone` (row already `FAILED`) -> the transaction ends with an exception (the fake unit of work records a rollback), the row stays `FAILED`, `run` does not throw.
- All rows in one currency -> `skippedCount` 0 and `notSavedMovements` empty.

**U7 `FindStatementImportInputPortTest`** (`find(FindStatementImportCommand)` -> `StatementImportResultDTO`; fixed `Clock` at 2026-11-01T10:00:00Z; `staleAfter` 10 min; Mockito for `ProductLifecycleService`, `MovementQueryRepository`; fake `InMemoryStatementImportRepository`)
- `findOrThrowByUserAndProductId` throws -> same exception; repository has no interaction.
- Unknown import id -> `BusinessException(STATEMENT_IMPORT_NOT_FOUND)`.
- Import row of another product -> `STATEMENT_IMPORT_NOT_FOUND`.
- `PROCESSING` started 2 min ago -> status `PROCESSING`, `movements` empty, `summary` null, no call to `MovementQueryRepository`.
- `PROCESSING` started 11 min ago -> status `FAILED`, `errorCode` `STATEMENT_IMPORT_TIMED_OUT`, movements empty. The stored row is unchanged.
- `PROCESSING` started exactly 10 min ago -> `PROCESSING` (stale only when older than the limit).
- `FAILED` with `STATEMENT_READER_FAILED` -> status `FAILED`, that code, movements empty.
- `DONE` with 3 movements returned as dates 2026-09-03, 2026-09-01, 2026-09-02 -> result order 09-01, 09-02, 09-03. Two movements on the same date keep `createdAt` order.
- `DONE` movement mapping: DEPOSIT 1000.00 -> `direction` CREDIT, `amount` 1000.00; WITHDRAWAL -20.00 -> `direction` DEBIT, `amount` 20.00 (positive); category alias and display name map; `confidence` 0.4 and `needsReview` true read from metadata (numbers read back from JSON as `Double`/`Integer` still give `0.4`); a movement with no review metadata -> `needsReview` false, `confidence` null.
- `DONE` -> `summary` is the stored summary.

**U8 `ProcessMovementOptionsVOTest`** (domain)
- `forStatementImport()` -> `skipFundsCheck` true, `operation` ADD, `isMonthOfficiallyReported` false.
- `new ProcessMovementOptionsVO(EntityOperationVO.ADD)` -> `skipFundsCheck` false.
- `new ProcessMovementOptionsVO(false, EntityOperationVO.REMOVE)` -> `skipFundsCheck` false.

**U9 `ProductDomainTest`** (domain, extend; `syncBalancesByMovement`)
- SAVINGS, current balance 0.00, withdrawal -50.00, default options -> `BusinessException(INSUFFICIENT_FUNDS)` (unchanged).
- Same product, withdrawal -50.00, `forStatementImport()` -> no exception, current balance -50.00, movement balance -50.00.
- CREDIT_CARD, balance 0.00, withdrawal -500.00, `forStatementImport()` -> no exception, balance -500.00.

**U10 `ProductTypeTest`** (domain)
- `allowsStatementImport()` is true for SAVINGS and CREDIT_CARD; false for REAL_ESTATE_INVESTMENT, LOAN, INVESTMENT, CDT.

**U11 `MovementMetadataTest`** (domain)
- `putStatementReview(new MovementReviewVO(new BigDecimal("0.9"), false))` -> `get(STATEMENT_CONFIDENCE)` = `0.9`, `get(STATEMENT_NEEDS_REVIEW)` = `false`.

**U12 `AddMovementUploadedFileCommandTest`**
- The 3-argument and 4-argument constructors give `category`, `description`, `statementImportId`, `review` all `null`.
- `validate()` is unchanged: null date -> `IllegalArgumentException`; null amount and null snapshot -> `IllegalArgumentException`.

**U13 `AddMovementsUploadedFileInputPortStatementTest`** (`uploadMovementsFromFile(userId, productId, UploadMovementsBatchCommand)`; Mockito for `ProductLifecycleService`, `MovementWriterRepository`, `MonthlyBalanceSyncForUploadedMovements`; `RecordingUnitOfWork`; `CategoryServiceMock`)
- Command with category `EXP_GROCERY`, description `TIENDA`, import id X, review (0.9,false), WITHDRAWAL -50.00 -> the `MovementDTO` handed to `movementRepo.save` has that category, description `TIENDA`, `statementImportId` X, metadata `STATEMENT_CONFIDENCE` 0.9 and `STATEMENT_NEEDS_REVIEW` false, and `FILE_IMPORT_TAG` true.
- Command without category, DEPOSIT -> category Other income (as today). Without category, WITHDRAWAL -> Unknown expense (as today). Description and import id `null`.
- Batch with `forStatement` options -> `syncByUploadedMovements(domain, movements, options)` receives `skipFundsCheck` true. Batch from the old 3-argument method -> `skipFundsCheck` false.
- Product lock is still taken first (the existing `AddMovementsUploadedFileInputPortLockTest` keeps its expectation; its stub changes to the 3-argument `syncByUploadedMovements`).

### Integration tests (opt-in `@QuarkusTest`, tag `database`, real PostgreSQL, run with `-Djbh.test.excludedGroups=none`)

Setup: the bean `ExtractStatementMovementsUseCase` is replaced by a mock (`@InjectMock`) that
returns a canned `StatementClassificationDTO`. The user lookup is stubbed the way the existing
infra `@QuarkusTest` classes do it. A `SAVINGS` product starts with current balance `0.00`. The PDF
is any small valid PDF (the reader is mocked). Poll with a bounded wait (up to 10 s), never a fixed sleep.

Canned classification C1 (PDF order): `TIENDA` 2026-09-03 DEBIT 50.00 COP `EXP_GROCERY` 0.9;
`NOMINA` 2026-09-01 CREDIT 1000.00 COP `INC_SALARY` 0.95; `XYZ` 2026-09-02 DEBIT 20.00 COP
Unknown expense 0.4 (needs review); `AMAZON` 2026-09-02 DEBIT 5.00 USD `EXP_SHOPPING` 0.9. Balances `[COP]`.

- **IT1 happy path** (canned list C1 also holds `FUTURE` 2099-01-01 DEBIT 9.00 COP `EXP_GROCERY` 0.9, so the DB date check is never hit). POST with PDF-1 -> `202`, body has `importId` and `status` `PROCESSING`. Poll the GET until `status` is not `PROCESSING`: `DONE`; `movements` has 3 items in order `NOMINA`, `XYZ`, `TIENDA` (09-01, 09-02, 09-03); `NOMINA` direction CREDIT amount 1000.00 category alias `INC_SALARY`; `XYZ` DEBIT 20.00 category alias = Unknown expense, `needsReview` true, `confidence` 0.4; `summary.mainCurrency` `COP`, `savedCount` 3, `skippedCount` 2, `needsReviewCount` 1, `notSavedMovements` = [`AMAZON`, USD, `OTHER_CURRENCY`; `FUTURE`, COP, `FUTURE_DATE`]. The product current balance is `930.00` (GET product). The DB has 3 movements with `statement_import_id` = the import id, and 0 for `AMAZON` and `FUTURE`.
- **IT2 duplicate.** POST PDF-1 again to the same product -> `409`, error `STATEMENT_ALREADY_IMPORTED`. POST PDF-1 to a second `SAVINGS` product of the same user -> `202`. POST a different PDF-2 to the first product -> `202`.
- **IT3 type and owner.** POST to a `CDT` product -> `422`, `STATEMENT_PRODUCT_TYPE_NOT_ALLOWED`, no row in `statement_imports`. POST to another user's product -> `422`, `PRODUCT_NOT_FOUND`, no row.
- **IT4 failed import then retry.** The mock throws `BusinessException(STATEMENT_READER_FAILED)` for PDF-3. POST -> `202`; poll -> `FAILED`, `errorCode` `STATEMENT_READER_FAILED`, `movements` `[]`, `summary` null. Then the mock works; POST PDF-3 again -> `202` with a different `importId` (not `409`). The first row stays `FAILED`.
- **IT5 stale.** Insert a `PROCESSING` row for PDF-4 with `started_at` = now - 11 min (SQL). GET -> `FAILED`, `STATEMENT_IMPORT_TIMED_OUT`; the DB row is still `PROCESSING`. POST PDF-4 -> `202`; the old row is now `FAILED` with `STATEMENT_IMPORT_TIMED_OUT`, a new row is `PROCESSING`/`DONE`. With a row `PROCESSING` for 2 min (PDF-5) -> POST PDF-5 -> `409`.
- **IT6 finish after stale.** Call `StatementImportRepository.markDone` for a row that is already `FAILED` -> returns `false`, the row is unchanged. Run the run use case with a stale-failed row -> no movement with that `statement_import_id` stays in the DB and the product balance is unchanged.
- **IT7 funds check skipped.** `CREDIT_CARD` product, balance 0.00, canned DEBIT 500.00 COP -> `DONE`, current balance `-500.00`.
- **IT8 same PDF at the same time.** Two threads call `StatementImportRepository.insertProcessing` with the same product and hash -> exactly one returns `true`; the table has one non-`FAILED` row.
- **IT9 poll owner check.** GET with another user's token -> `422` `PRODUCT_NOT_FOUND`. GET with a random import id -> `422` `STATEMENT_IMPORT_NOT_FOUND`. GET with the import id under a different product of the same user -> `422` `STATEMENT_IMPORT_NOT_FOUND`.
- **IT10 old endpoint.** `POST /jbh-api/finance/statements/extract` -> `404`.
- **IT11 migration.** After start-up, `finance.statement_imports` exists, `uk_statement_imports_product_pdf_active` is a unique partial index, `finance.movements.statement_import_id` exists and is nullable.

## Complexity and size notes

- `AddMovementsUploadedFileInputPort.mapCommandToDomain` already holds a lambda with an
  `if / else if` and a `try / catch`. Before the new fields are added, move one movement's mapping
  into a private method `toMovementDomain(command, accountDomain, importedAt)` and the category
  choice into `resolveCategory(command)`. The new branches (category present, review, import id)
  go into those helpers, each under 40 lines and cyclomatic 10.
- `ExtractStatementMovementsInputPort.categorize` grows a per-direction fallback. Move the
  per-row decision into a private method `categorizeOne(...)`. The loop stays short.
- `RunStatementImportInputPort.run`: keep the orchestration to about 12 lines; the row mapping,
  the summary and the failure marking are private methods.
- Files that may pass 400 lines: `ProductDomain` (existing, only one line changes there),
  `AddMovementsUploadedFileInputPort` (about 200 lines now). Keep them under 400.

## Risks and open questions

- **Credit card payments.** `FinanceBusinessRules.md` says credit card deposits are made by
  transfer. The upload path does not run the product-type movement validator, so CREDIT rows
  (card payments) are saved as deposits with the Other income / income category. Accepted by D5.
- **Hibernate on a virtual thread** with a JDBC transaction is already used by the existing
  category read and the monthly balance flow. No new risk is added, but the integration tests
  IT1 and IT7 are the first to run a locked multi-statement transaction on that thread.
- **Clock use.** The stale UPDATE uses the database clock. The poll uses the application clock
  (`java.time.Clock`). The difference matters only within the clock skew around the 10-minute edge.
- **Existing tests that must change** (mechanical, they do not lose any assertion):
  `AddMovementsUploadedFileInputPortLockTest` (stub of `syncByUploadedMovements` moves to the
  3-argument form), and the two U4 tests listed above, whose expected values follow D2.
  Other callers of `new CategorizedMovementDTO(` and `new StatementClassificationDTO(` (added
  components) are found by the leader's build.
