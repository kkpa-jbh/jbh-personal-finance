# Proposal: jbh-finance-save-statement-movements

Source ticket: JBH-73 - https://linear.app/kkpa/issue/JBH-73/save-imported-statement-movements-to-a-product

## Why

Today the bank statement import reads a PDF, finds the movements, and suggests a category for
each one. Then it only writes the result to the log. The user gets nothing back and nothing is
saved. The user wants to save these movements into a product, and then see them in a table,
sorted by date, with a category label for each row.

Three gaps stop this today:

1. The upload endpoint does not know the product. The statement belongs to one product, so the
   product id must be part of the API.
2. Money in gets the wrong "Unknown" category (the expense one), and a low-confidence Jev answer
   keeps Jev's pick instead of falling back to Unknown.
3. Nothing remembers an import. There is no id to poll, no duplicate check, and no link from a
   saved movement to the import that created it.

## What Changes

- **New API (product in the path).**
  `POST /jbh-api/finance/products/{productId}/statement-imports` (multipart `file`, optional
  `password`) answers `202 {importId, status}`.
  `GET /jbh-api/finance/products/{productId}/statement-imports/{importId}` answers the status, the
  error code, a summary, and the saved movements sorted by date, each with its category label.
- **Remove** `POST /jbh-api/finance/statements/extract` and its response classes.
- **Background job** (already on a virtual thread) now saves the movements through the existing
  `AddMovementsUploadedFileUseCase` (one transaction, product row lock first, product balance and
  monthly balance sync). The import status moves to `DONE` in the same transaction.
- **Unknown rule.** No valid Jev answer, or confidence under `min-confidence` (0.7): money in gets
  *Other income*, money out gets *Unknown expense*.
- **Main currency.** Only rows in the statement's main currency are saved. Other rows are returned
  in the poll answer as "not saved".
- **Duplicates.** The same PDF (SHA-256 of the uploaded bytes) cannot be imported twice into one
  product: `409`. A `FAILED` import does not block a retry.
- **Product types.** Only `SAVINGS` and `CREDIT_CARD` accept a statement import (`422` otherwise).
- **No funds check** for statement imports (the bank already accepted the movements).
- **Stuck jobs.** A `PROCESSING` import older than 10 minutes (config) counts as `FAILED`.
- **Database (breaking for nobody, additive).** One new table `finance.statement_imports` and one
  new nullable column `finance.movements.statement_import_id` with a partial index.

## Capabilities

### New Capabilities

- `finance-statement-import`: save the movements of a bank statement PDF into a product, poll the
  result, and the rules around duplicates, currency, categories and stuck jobs.

### Modified Capabilities

None. No capability for the old extract endpoint exists in `openspec/specs/`.

## Impact

- **Breaking:** yes, for API clients. `POST /jbh-api/finance/statements/extract` is removed. The
  only client is `jbh-app` (import statement page), which already shows an error toast for the
  `202` answer, and is replaced in a separate change.
- **Modules affected:** `jbh-finance` only (`jbh-finance-domain`, `jbh-finance-application`,
  `jbh-finance-infra`). No cross-module change. No gateway change: the new paths stay under
  `/jbh-api/finance/**`.
- **Database:** yes. Read paths touched:
  - new: poll read of one import row by id; poll read of the movements of one import (partial index);
  - new: duplicate and stale checks on `statement_imports` by (product, SHA-256);
  - existing and unchanged in shape: the bulk movement insert, product balance and monthly balance
    sync used by the upload flow. The new nullable column adds no cost to the existing movement
    reads (`idx_movements_product_date` is not changed).
- **Not included:** the `jbh-app` front end (product picker, upload call, polling, table). That is
  a separate later change in the `jbh-app` repo. Also not included: deleting or listing past
  imports, storing the PDF, a scheduler, a database job queue.
- **Knowledge base:** after this change, run `kkpa-context-curate` for
  `kkpa/context/use-case/jbh-finance/extract-statement-movements.md` and
  `kkpa/context/input-port/jbh-finance/import-statement.md`. Do not edit them by hand.
