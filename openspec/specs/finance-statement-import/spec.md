# finance-statement-import Specification

## Purpose
TBD - created by archiving change jbh-finance-save-statement-movements. Update Purpose after archive.
## Requirements
### Requirement: A statement import is started for one product

The system MUST accept a bank statement PDF only for a product that belongs to the signed-in user
and whose type is `SAVINGS` or `CREDIT_CARD`. On success it MUST answer `202` with an import id and
the status `PROCESSING`, and it MUST do the work in the background.

#### Scenario: Upload to a savings product

- **GIVEN** the user owns a `SAVINGS` product
- **WHEN** the user uploads a valid PDF to `POST /jbh-api/finance/products/{productId}/statement-imports`
- **THEN** the answer is `202` with `importId` and `status` `PROCESSING`

#### Scenario: Upload to a product type that is not allowed

- **GIVEN** the user owns a `CDT`, `LOAN`, `INVESTMENT` or `REAL_ESTATE_INVESTMENT` product
- **WHEN** the user uploads a valid PDF to that product
- **THEN** the answer is `422` with the error `STATEMENT_PRODUCT_TYPE_NOT_ALLOWED` and nothing is stored

#### Scenario: Upload to a product of another user

- **GIVEN** the product belongs to another user
- **WHEN** the user uploads a valid PDF to it
- **THEN** the answer is `422` with the error `PRODUCT_NOT_FOUND` and nothing is stored

#### Scenario: The old endpoint is gone

- **WHEN** a client calls `POST /jbh-api/finance/statements/extract`
- **THEN** the answer is `404`

### Requirement: The same PDF cannot be imported twice into one product

The system MUST compute the SHA-256 of the uploaded PDF bytes. While an import of the same PDF in
the same product is `PROCESSING` or `DONE`, a second upload MUST be refused with `409`. A `FAILED`
import MUST NOT block a new upload. The same PDF MAY be imported into a different product.

#### Scenario: Second upload of the same PDF

- **GIVEN** an import of PDF X in product P is `DONE`
- **WHEN** the user uploads PDF X to product P again
- **THEN** the answer is `409` with the error `STATEMENT_ALREADY_IMPORTED`

#### Scenario: Retry after a failed import

- **GIVEN** an import of PDF X in product P is `FAILED`
- **WHEN** the user uploads PDF X to product P again
- **THEN** the answer is `202` with a new `importId`

#### Scenario: Same PDF in another product

- **GIVEN** an import of PDF X in product P is `DONE`
- **WHEN** the user uploads PDF X to product Q
- **THEN** the answer is `202`

#### Scenario: Two uploads of the same PDF at the same time

- **WHEN** two requests with the same PDF for the same product arrive at the same time, on the same
  or on different instances
- **THEN** exactly one answers `202` and the other answers `409`

### Requirement: Movements are saved to the product

When the background job ends well, the system MUST save the movements of the statement's main
currency into the product in one transaction, with the product balance and the monthly balances
synchronized, and MUST set the import status to `DONE` in that same transaction. A money-in row
(CREDIT) MUST be saved as a deposit. A money-out row (DEBIT) MUST be saved as a withdrawal. The
rows MUST be applied in date order. The insufficient-funds check MUST NOT run for a statement import.

#### Scenario: Movements are saved and the balance changes

- **GIVEN** a `SAVINGS` product with current balance `0.00`
- **WHEN** an import saves a CREDIT of `1000.00` and two DEBITs of `50.00` and `20.00`
- **THEN** the product current balance is `930.00` and the import status is `DONE`

#### Scenario: Funds check is skipped

- **GIVEN** a `CREDIT_CARD` product with current balance `0.00`
- **WHEN** an import saves a DEBIT of `500.00`
- **THEN** the movement is saved and the current balance is `-500.00`

#### Scenario: Status and movements appear together

- **WHEN** a client polls while the job is saving
- **THEN** it sees `PROCESSING` with no movements, or `DONE` with all movements, never `DONE`
  with missing movements

### Requirement: Unknown category rule

A movement MUST get the fallback category when the classifier gives no valid answer for it, or
when the answer confidence is under `min-confidence` (0.7). For money in (CREDIT) the fallback is
the *Other income* category. For money out (DEBIT) it is the *Unknown expense* category. A
movement with the fallback category MUST be marked "needs review".

#### Scenario: Low confidence on money out

- **WHEN** Jev answers `EXP_GROCERY` with confidence `0.5` for a DEBIT
- **THEN** the movement gets the Unknown expense category, keeps confidence `0.5`, and needs review

#### Scenario: Low confidence on money in

- **WHEN** Jev answers `INC_SALARY` with confidence `0.4` for a CREDIT
- **THEN** the movement gets the Other income category and needs review

#### Scenario: No valid answer on money in

- **WHEN** Jev gives no answer, or an expense category, for a CREDIT
- **THEN** the movement gets the Other income category, confidence `0`, and needs review

#### Scenario: Confidence equal to the minimum

- **WHEN** Jev answers a valid category with confidence `0.7`
- **THEN** the movement keeps Jev's category and does not need review

### Requirement: Only the main currency is saved

The system MUST take as the main currency the currency with the most rows. On a tie it MUST take
the currency of the first balance section. Rows in other currencies MUST NOT be saved. They MUST be returned by the poll
answer, marked as not saved.

#### Scenario: One foreign row

- **GIVEN** a statement with 3 COP rows and 1 USD row
- **WHEN** the import ends
- **THEN** the 3 COP rows are saved, the USD row is in `summary.notSavedMovements`, and
  `summary.mainCurrency` is `COP`

#### Scenario: Tie broken by the first balance section

- **GIVEN** a statement with 2 COP rows, 2 USD rows, and the balance sections listed as USD then COP
- **WHEN** the import ends
- **THEN** `summary.mainCurrency` is `USD`

### Requirement: Rows dated in the future are skipped

The system MUST NOT save a statement row whose date is after today. It MUST skip that row, return
it in `summary.notSavedMovements` with the reason `FUTURE_DATE`, and MUST save the other rows. The
reason of a skipped row is `OTHER_CURRENCY` or `FUTURE_DATE`; a row in another currency always
gets `OTHER_CURRENCY`.

#### Scenario: One row has a future date

- **GIVEN** a statement with rows dated 2026-09-01 and 2026-11-02, and today is 2026-11-01
- **WHEN** the import ends
- **THEN** the import is `DONE`, the 2026-09-01 row is saved, and the 2026-11-02 row is in
  `summary.notSavedMovements` with the reason `FUTURE_DATE`

#### Scenario: A row dated today

- **WHEN** a row is dated today
- **THEN** it is saved

#### Scenario: Every row is in the future

- **GIVEN** every row in the main currency is dated after today
- **WHEN** the import ends
- **THEN** the import is `DONE` with `savedCount` 0, and all rows are in `summary.notSavedMovements`

### Requirement: The client polls the result

`GET /jbh-api/finance/products/{productId}/statement-imports/{importId}` MUST answer the status
(`PROCESSING`, `DONE`, `FAILED`), the error code, the summary, and the saved movements sorted by
date ascending. Each movement has date, description, amount (positive), direction, the category
label per language, confidence and needs-review. The owner check MUST use the product in the path
and the signed-in user. The answer MUST NOT depend on which instance ran the job.

#### Scenario: Poll a finished import

- **GIVEN** an import is `DONE` with 3 saved movements dated 2026-09-03, 2026-09-01 and 2026-09-02
- **WHEN** the owner calls the GET
- **THEN** the movements come in the order 2026-09-01, 2026-09-02, 2026-09-03

#### Scenario: Poll a failed import

- **GIVEN** the reader failed
- **WHEN** the owner calls the GET
- **THEN** the status is `FAILED`, the error code is `STATEMENT_READER_FAILED`, and the movements
  list is empty

#### Scenario: Poll an import of another product or user

- **WHEN** the import id does not belong to the product in the path, or the product is not the user's
- **THEN** the answer is `422` (`STATEMENT_IMPORT_NOT_FOUND` or `PRODUCT_NOT_FOUND`)

### Requirement: A stuck import counts as failed

A `PROCESSING` import older than the configured time (default 10 minutes) MUST be reported as
`FAILED` with the error code `STATEMENT_IMPORT_TIMED_OUT`. A new upload of the same PDF MUST first
mark the stale row `FAILED`, then start a new import.

#### Scenario: Poll a stale import

- **GIVEN** an import has been `PROCESSING` for 11 minutes
- **WHEN** the owner calls the GET
- **THEN** the status is `FAILED` with the error code `STATEMENT_IMPORT_TIMED_OUT`

#### Scenario: Re-upload over a stale import

- **GIVEN** an import of PDF X has been `PROCESSING` for 11 minutes
- **WHEN** the user uploads PDF X again
- **THEN** the answer is `202`, the old row is `FAILED`, and a new row is `PROCESSING`

#### Scenario: Re-upload while the import is still fresh

- **GIVEN** an import of PDF X has been `PROCESSING` for 2 minutes
- **WHEN** the user uploads PDF X again
- **THEN** the answer is `409`

#### Scenario: The job ends after the row was marked stale

- **GIVEN** a stale row was marked `FAILED` while its job was still running
- **WHEN** that job finishes saving
- **THEN** its transaction rolls back, no movement of that import stays saved, and the status stays `FAILED`

