# Bank statement import — POST /jbh-api/finance/statements/extract

> One external entry point, one output. **Backend only** — the page and gateway side live in
> the input-port file that links here. Paths + symbols only; ask CodeGraph for signatures, never
> record line numbers. All KB links below are relative to `kkpa/context/`.
>
> Paths are relative to the `jbh-personal-finance` repo root, except `jbh-deploy/...`, which is
> the sibling repo in the `kkpa-jbh` folder. `AI` below = `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/out/ai/`.

**Known as:** bank statement import, import statement, extracto bancario, upload monthly bank
report, statement upload. **Internal name:** `ExtractStatementMovementsUseCase`.
Built by ticket JBH-43. Reader redesigned on 2026-10-01 (Gemini free tier answered 503): now
OCR + LLM, both swappable. **Async since 2026-10-02 (temporary, for provider testing):** the call
answers `202` at once and the import runs in the background; the result is only logged and, in
dev, written to text files. Currency per movement and balances per currency since 2026-10-02.

## Entry point

- **Symbol:** `StatementRestAdapter.extractMovements` — `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/rest/statement/StatementRestAdapter.java`
- **Trigger:** `POST /jbh-api/finance/statements/extract` (`FinanceApiRoutes.STATEMENTS_API_PATH` + `STATEMENTS_EXTRACT_API`)
- **Module:** `jbh-finance`

## Triggered by

- `input-port/jbh-finance/import-statement.md` — the import statement page in `jbh-app`

## Input / output

- **Input:** `multipart/form-data` with `file` (the PDF, required) and `password` (only for a
  locked PDF, never saved or logged), plus the `Authorization` header (JWT).
- **Output (now, temporary):** `202` with `{"status":"PROCESSING"}` as soon as the PDF is checked
  and unlocked. Errors still answered at once: `401` (no valid user), `422` for the file and
  password errors (`STATEMENT_FILE_*`, `STATEMENT_UNREADABLE_PDF`, `STATEMENT_PASSWORD_REQUIRED`,
  `STATEMENT_WRONG_PASSWORD`). Every later error (AI not configured, reader/classifier failed, no
  movements) only reaches the log (`[StatementImport:Async] Failed: <code>`).
- **Result (not sent today):** `StatementClassificationDTO` — the movements in PDF order, each
  with a currency, a suggested category, a confidence and a "needs review" flag, plus one totals
  check per currency (`MATCH`, `MISMATCH`, `NOT_AVAILABLE`). `StatementClassificationResponse`
  (`movements`, `totalsChecks` of `TotalsCheckResponse`) still maps it, but no endpoint returns it
  now.

## Flow

1. `StatementRestAdapter.extractMovements` — `.../adapters/in/rest/statement/StatementRestAdapter.java` — gets the user id from `jbh-iam` (`BaseRestAdapter.findUserId`), checks the file is present, is a PDF, and is under `jbh.statement.max-file-size-bytes`, then reads the bytes.
2. `PdfStatementUnlocker.unlock` — `.../adapters/in/service/PdfStatementUnlocker.java` — removes the PDF password in memory with PDFBox. Returns the same bytes when the PDF is not encrypted.
   Then `StatementImportDispatcher.dispatch` — `.../adapters/in/service/StatementImportDispatcher.java` — starts the import on a new virtual thread, and the REST call answers `202`.
   On that thread: `StatementImportRunner.run` — `.../adapters/in/service/StatementImportRunner.java` — `@ActivateRequestContext`; calls the use case, logs `Finished: N movements` or `Failed: <code>`, and calls `JevReport.write` (step 9).
3. `ExtractStatementMovementsInputPort.extract` — `jbh-finance/jbh-finance-application/src/main/java/com/jbh/finance/application/feature/statement/ports/input/ExtractStatementMovementsInputPort.java` — the use case (interface `ExtractStatementMovementsUseCase`, command `ExtractStatementMovementsCommand`). Owns the 4 steps below and logs each one.
4. **Step 1/4 read statement:** `StatementReader.read` → `OcrLlmStatementReaderAdapter` — `AI/OcrLlmStatementReaderAdapter.java` — two swappable infra steps:
   - **1a PDF → text** (`StatementTextSource`, chosen by `jbh.statement.text-source`):
     - `AzureLayoutTextSourceAdapter` (default, `azure`) — `AI/AzureLayoutTextSourceAdapter.java` — `PdfStatementSplitter` (`AI/PdfStatementSplitter.java`) cuts the PDF into parts of `azure.pages-per-request` pages; each part goes to Azure AI Document Intelligence `prebuilt-layout`, polled until done; the markdown of all parts is joined.
     - `PdfBoxTextSource` (`pdfbox`) — `AI/PdfBoxTextSource.java` — local text extraction, no OCR.
   - **1b text → movements** (`StatementLlm`, chosen by `jbh.statement.llm`):
     - `OpenRouterLlmAdapter` (default, `openrouter`) — `AI/llm/OpenRouterLlmAdapter.java` — `/chat/completions` with a `models` fallback list and a strict `json_schema`.
     - `GeminiLlmAdapter` (`gemini`) — `AI/llm/GeminiLlmAdapter.java` — `generateContent` with a `responseSchema`.
     - Both use `StatementExtractionFormat` — `AI/llm/StatementExtractionFormat.java` — the shared system prompt, user prompt and JSON → `ExtractedStatementDTO` parsing (movements with `currency`, plus `balances` per currency).
     - When `jbh.statement.compare.enabled` is on, the active LLM is wrapped by `ComparingStatementLlm` (`AI/llmcompare/`, built by `LlmComparison.wrap` in `StatementUseCasesCDIConfig`): the import gets the active result unchanged, and every other `StatementLlm` (Gemini, `OllamaLlmAdapter`, …) gets the same text in the background. See "LLM comparison" in the gotchas.
   - Zero movements → `STATEMENT_NO_MOVEMENTS_FOUND`.
5. **Step 2/4 check totals:** `StatementTotalsChecker.check` — `jbh-finance/jbh-finance-application/.../statement/services/StatementTotalsChecker.java` — one check per currency: compares credits minus debits of that currency's movements with its closing minus opening balance (either sign matches). A currency with no balances → `NOT_AVAILABLE`.
6. **Step 3/4 load categories:** `StatementCategoryCatalog.load` — `jbh-finance/jbh-finance-application/.../statement/services/StatementCategoryCatalog.java` — the active income and expense categories, and the Unknown category, as the allowed options per direction.
7. **Step 4/4 classify movements:** `MovementClassifier.classify` → `JevMovementClassifierAdapter` — `AI/JevMovementClassifierAdapter.java` — Jev (`/v1/systemone`, Choice questions) picks one category per movement, in batches of `jbh.statement.jev.batch-size`.
8. The use case returns `StatementClassificationDTO` to `StatementImportRunner` (not to the REST call).
9. `JevReport.write` — `AI/llmcompare/JevReport.java` — only when the comparison is on: writes `<time>-jev.txt` with the result and the Jev batches recorded by `JevTrace`.

Wiring and config:

| File | Role |
|---|---|
| `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra/StatementUseCasesCDIConfig.java` | Produces the use case; builds `OcrLlmStatementReaderAdapter` from the configured text source and LLM (`@Any Instance<StatementTextSource>`, `@Any Instance<StatementLlm>` + `select(Class)`), wrapped by `llmComparison.wrap(...)` (a no-op when the comparison is off) |
| `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/service/StatementImportDispatcher.java`, `StatementImportRunner.java` | Async: virtual-thread executor, and the bean that runs the use case in an activated request context |
| `AI/llmcompare/` | Temporary LLM comparison: `LlmComparison`, `ComparingStatementLlm`, `OllamaLlmAdapter`, `LlmCompareConfig` (`jbh.statement.compare.*`), `LlmCompareReport`, `ProviderRun`, `JevReport`, `JevTrace` |
| `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/rest/statement/response/` | `StatementClassificationResponse`, `CategorizedMovementResponse`, `TotalsCheckResponse` (unused while the call answers `202`) |
| `AI/StatementTextSource.java`, `AI/llm/StatementLlm.java` | Infra seams (not application ports) for step 1a and 1b |
| `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra/config/StatementImportConfig.java` | `@ConfigMapping(prefix = "jbh.statement")`: `text-source` (`TextSource` enum), `llm` (`Llm` enum), `azure.*`, `openrouter.*`, `gemini.*`, `jev.*` |
| `jbh-finance/jbh-finance-infra/src/main/resources/application.properties` | `jbh.statement.*` values and env vars: `STATEMENT_TEXT_SOURCE`, `STATEMENT_LLM`, `AZURE_DI_ENDPOINT`, `AZURE_DI_API_KEY`, `AZURE_DI_PAGES_PER_REQUEST`, `OPENROUTER_API_KEY`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `JEV_API_KEY`; model list override `JBH_STATEMENT_OPENROUTER_MODELS`. Comparison: `STATEMENT_LLM_COMPARE` (default `false`, `true` in `%dev`), `STATEMENT_LLM_COMPARE_DIR` (default `target/llm-compare`), `OLLAMA_API_KEY`, model override `JBH_STATEMENT_COMPARE_OLLAMA_MODEL` (default `gpt-oss:120b`) |
| `jbh-z-assembly/src/main/resources/application.properties` | Upload folder `/tmp/jbh-uploads`, delete uploads when the response is sent |
| `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/exceptions/FinanceInfraBusinessExceptionType.java` | `STATEMENT_*` infra errors (file, password, no text, AI not configured, reader/classifier failed) |
| `jbh-finance/jbh-finance-application/src/main/java/com/jbh/finance/application/shared/exceptions/BusinessApplicationExceptionType.java` | `STATEMENT_NO_MOVEMENTS_FOUND` |
| `jbh-finance/jbh-finance-application/src/main/java/com/jbh/finance/application/feature/statement/ports/output/StatementReader.java` | Application output port: unlocked PDF → movements |
| `jbh-finance/jbh-finance-application/src/main/java/com/jbh/finance/application/feature/statement/ports/output/MovementClassifier.java` | Application output port for the classifier |
| `jbh-finance/jbh-finance-application/src/main/java/com/jbh/finance/application/feature/statement/dto/` | DTOs: `ExtractedStatementDTO`, `ExtractedMovementDTO`, `StatementBalanceDTO`, `StatementMovementDirection`, `TotalsCheckDTO`, `ClassificationQuestionDTO`, `ClassificationAnswerDTO`, `CategorizedMovementDTO`, `StatementClassificationDTO` |
| `jbh-finance/jbh-finance-infra/pom.xml` | PDFBox dependency (`pdfbox.version`): unlock, split, text |
| `jbh-deploy/compose.yaml`, `jbh-deploy/.env.example`, `jbh-deploy/README.md` | Production env vars (same names as above, without `JBH_STATEMENT_OPENROUTER_MODELS`) |
| `jbh-finance/jbh-finance-application/src/test/java/com/jbh/finance/test/application/feature/statement/` | Unit tests (input port, command, catalog, totals checker, settings, direction) |

## Database

| # | Op | Table / entity | Where |
|---|---|---|---|
| 1 | READ | `finance.categories` (Unknown expense category) | `CategoryService.findExpenseUnknown` |
| 2 | READ | `finance.categories` (active, per source: income, expense) | `CategoryQueryRepo.findActiveBySource` (`CategoryRepositoryAdapter`) |

Nothing is written. The result is only returned.

## Entities involved

No entity guide exists yet. Categories are the only entity read (`CategoryJPAEntity`,
`finance.categories`).

## Related use cases

None in the KB yet. The page does not save the movements; saving them is a separate flow.

## Conventions & gotchas

### Adding or swapping a provider

- **A new OCR or LLM provider is one class plus one enum value.** Implement `StatementTextSource` in `AI/`
  or `StatementLlm` in `AI/llm/`, add a value to `StatementImportConfig.TextSource` or `Llm`, and a
  `case` in `StatementUseCasesCDIConfig`. The application layer and its tests do not change.
  _Source: `StatementUseCasesCDIConfig`, `StatementLlm`, `StatementTextSource`._
- **Use `Instance` + `select(Class)`, never a plain `@Inject StatementLlm`.** Each seam has
  several beans, so a plain injection is ambiguous. ArC keeps beans that match an `Instance`
  injection point, so none is removed as unused. _Source: Quarkus CDI reference, "Removing
  Unused Beans"._
- **Prompt rules are bank-independent.** No labels, column names or layouts from one statement
  (user decision, 2026-10-01). The LLM decides which column is the date, the description and
  the amount. A rule change goes in `StatementExtractionFormat` once, for all LLMs.
  **Exception (user decision, 2026-10-02):** the balance rule may give *common* labels used by many
  banks as examples ("Saldo anterior", "Saldo total", "Pago total", "New balance"), never one
  bank's full dictionary. _Source: `StatementExtractionFormat`._
- **Every LLM uses `StatementExtractionFormat`** for `SYSTEM_PROMPT`, `userPrompt(...)` and
  `parse(...)`. Each provider only adds its own JSON schema dialect (OpenRouter: JSON Schema,
  lower case types; Gemini: OpenAPI subset, upper case types).
- **A provider implements only `StatementLlm.answer`** (the model's raw text). The default
  `extract` parses it with `StatementExtractionFormat.parse(String)`, which first removes a
  markdown code fence. _Source: `StatementLlm`._
- **A new field in the LLM answer touches 3 JSON shapes:** the OpenRouter `RESPONSE_SCHEMA`
  (JSON Schema, `additionalProperties: false`, every property `required`), the Gemini
  `RESPONSE_SCHEMA` (OpenAPI subset), and `OllamaLlmAdapter.JSON_SHAPE` (in the prompt, Ollama
  Cloud has no structured output). Plus the rule in `StatementExtractionFormat.RULES` and its
  `parse`. _Source: currency change, 2026-10-02._

### Async (temporary, since 2026-10-02)

- **The call answers `202` before the AI work starts**, so no HTTP timeout (the app waited 150 s)
  can cut the OCR and LLM calls. The result is not returned; nothing is saved. When the result
  must go back to the app, add a result store and a `GET` (designed but not built: in-memory
  `jobId → CompletableFuture`, 30 min, owner check). _Source: `StatementRestAdapter`._
- **`@ActivateRequestContext` must sit on a separate bean** (`StatementImportRunner`), called
  through its CDI proxy. A background thread has no request context, and the category read
  (Hibernate, no `@Transactional`) needs one. _Source: Quarkus docs, `StatementImportRunner`._
- **The `jbh-app` page still expects the old `200` result**, so after the `202` it shows its
  generic error toast. Accepted while testing providers. _Source: user decision, 2026-10-02._
- **OpenRouter keeps its own limit** (`jbh.statement.openrouter.timeout-seconds`, 120). The async
  call removes only the HTTP wait of the client. A real run on 2026-10-02 took ~22 s (Azure, 5
  pages) + ~55 s (OpenRouter, nemotron answered) + <1 s (Jev).

### Currency and balances (since 2026-10-02)

- **Every movement has an ISO 4217 `currency`** taken from the section the row is in, or the main
  currency of the statement. A missing or invalid code becomes `XXX` (ISO "no currency"), never a
  dropped row. _Source: `StatementExtractionFormat.toCurrency`._
- **Balances are a list, one entry per currency section:** `{currency, openingBalance,
  closingBalance}`. A credit card PDF can hold a COP and a USD section, each with its own summary.
  _Source: `StatementBalanceDTO`._
- **Account vs credit card balances.** Account: start and end balance. Credit card: the balances are
  the debt — opening = previous balance ("Saldo anterior"), closing = total debt at the cut-off date
  ("Saldo total", "Pago total"). Never the minimum payment ("Pago mínimo") or the credit limit
  ("Cupo"). _Source: `StatementExtractionFormat.RULES`._
- **The totals check runs per currency**, so USD movements never break the COP check. The result is
  a list (`StatementClassificationDTO.totalsChecks`). _Source: `StatementTotalsChecker`._

### Azure AI Document Intelligence (step 1a, default)

- **OCR only: Azure gets the PDF bytes and no instructions.** It returns markdown; tables come
  back as HTML tables with their columns kept. _Source: `AzureLayoutTextSourceAdapter`._
- **The free tier (F0) reads only the first 2 pages of each request, max 4 MB, 500 pages per
  month.** That is why the PDF is split (`azure.pages-per-request`, default 2). A 5-page
  statement = 3 requests, 5 pages of the quota. `0` sends the whole PDF (paid tier).
  _Source: Microsoft Learn, Document Intelligence service limits._
- **Request:** `POST {endpoint}/documentintelligence/documentModels/prebuilt-layout:analyze?api-version=2024-11-30&outputContentFormat=markdown`,
  body `{"base64Source": ...}`, header `Ocp-Apim-Subscription-Key`. Answer `202` with an
  `Operation-Location` URL; poll it (`Retry-After`, else 1 s) until `status` is `succeeded`
  (`analyzeResult.content`) or `failed`. `429` is retried up to 3 times. `azure.timeout-seconds`
  (60) is the limit per part, polling included. _Source: `AzureLayoutTextSourceAdapter`._
- **Keys are per resource.** Azure portal → the Document Intelligence resource → Keys and
  Endpoint → `KEY 1` and `Endpoint`. Keys appear only after the deployment succeeds.
- An empty `AZURE_DI_ENDPOINT` or `AZURE_DI_API_KEY` → `STATEMENT_AI_NOT_CONFIGURED`. Blank
  content from all parts → `STATEMENT_PDF_HAS_NO_TEXT`.

### OpenRouter (step 1b, default)

- **`models` is a fallback list:** OpenRouter tries them in order and moves on when one is busy or
  fails. Default: `qwen/qwen3.8-27b:free`, `nvidia/nemotron-3-super-120b-a12b:free` (free models
  that support `structured_outputs` on 2026-10-01). Free models may disappear; check
  `https://openrouter.ai/models`. _Source: `application.properties`._
- **The model list has no `${ENV:default}` expression** because model ids contain `:`. Override
  it with env `JBH_STATEMENT_OPENROUTER_MODELS`. It is not passed by `compose.yaml`, because an
  empty env var may count as missing and stop the service. _Source: `application.properties`._
- **`response_format` is a strict `json_schema`, plus `provider.require_parameters: true`**, so
  providers that would ignore the schema are skipped. _Source: `OpenRouterLlmAdapter.buildBody`._
- **Free limits:** 20 requests/minute; 50/day without credits, 1,000/day after a one-time $10
  purchase. Free models can be slow (minutes) and **may train on the data** — check the
  OpenRouter privacy settings. _Source: OpenRouter docs, limits._
- A `200` answer can still carry an `error` object (all models failed late): it is treated as a
  failure. _Source: `OpenRouterLlmAdapter.answer`._

### Gemini (step 1b, optional)

- **The free tier answered `503` "This model is currently experiencing high demand"** even for a
  tiny prompt, on `gemini-flash-latest` and `gemini-3.8-flash`. That is why OpenRouter is the
  default. _Source: test on 2026-10-01._
- It now receives **text** (from step 1a), not the PDF. The model is the alias
  `gemini-flash-latest` (env `GEMINI_MODEL`), not a pinned version.
- Only `candidates[0].content.parts[0].text` is read; an empty answer (for example a safety
  block) → `STATEMENT_READER_FAILED`. _Source: `GeminiLlmAdapter.answerText`._

### Parsing (all LLMs)

- **Bad rows are skipped without an error.** A movement whose `date` is not `yyyy-MM-dd`, whose
  `amount` is not a plain decimal, or whose `direction` is not `CREDIT`/`DEBIT` is dropped; only
  the count is logged. A bad balance becomes `null`, so the totals check shows `NOT_AVAILABLE`.
  _Source: `StatementExtractionFormat.parse`._
- Every provider failure becomes `STATEMENT_READER_FAILED`; the cause is dropped on
  purpose (it may carry personal data). Read the log line to see which call failed.
- `STATEMENT_AI_NOT_CONFIGURED` is shared by Azure, OpenRouter, Gemini, Ollama and Jev, and is
  thrown before any HTTP call when the key is empty. The `Failed at Step n/4` line tells which
  provider it was (step 4 → `JEV_API_KEY`).

### LLM comparison (temporary, since 2026-10-02)

- **On by default only in the `dev` profile** (`%dev.jbh.statement.compare.enabled`); off in
  production (`compose.yaml` does not pass `STATEMENT_LLM_COMPARE`). When off, `LlmComparison.wrap`
  returns the active LLM unchanged. _Source: `application.properties`, `LlmComparison`._
- **Output:** `<output-dir>` is relative to the app's working folder — with `quarkus dev` that is
  `jbh-z-assembly/target/llm-compare/`. The log prints the absolute path
  (`[StatementImport:LlmCompare] … Results in <path>`, `Jev result in <path>`).
  - `<time>/input.txt` — the OCR text every LLM received.
  - `<time>/prompt.txt` — the system and user prompts as plain text.
  - `<time>/<Provider>-request.json` — the exact body each provider is sent (`StatementLlm.requestBody`; never the API key, keys travel in headers).
  - `<time>/<Provider>.txt` — time, error, and the raw answer.
  - `<time>-jev.txt` — the categorized movements (currency, category, confidence, review), the
    totals per currency, then each Jev batch's request and raw response (`JevTrace`, request scoped).
- **Every `StatementLlm` bean is compared**, found with `@Any Instance` handles; a new provider joins
  with no change in `llmcompare`. A provider without a key shows `STATEMENT_AI_NOT_CONFIGURED`.
- **The files hold real bank data.** Keep them under `target/` (git-ignored).
- **To remove it:** delete `AI/llmcompare/`, the `jbh.statement.compare.*` properties, the `wrap`
  call in `StatementUseCasesCDIConfig`, the `JevReport` call in `StatementImportRunner`, the
  `JevTrace` use in `JevMovementClassifierAdapter`, and `StatementLlm.requestBody` with its overrides.

### Jev (step 4)

- **Only the description and direction are sent**, never the amount. Model pinned to
  `jev-1.13.0`; `batch-size` 20; `min-confidence` 0.7; timeout 30 s. Under the minimum, or no
  valid answer (→ Unknown category), the movement is marked for review.
  _Source: `JevMovementClassifierAdapter`, `application.properties`._
- **Income categories for money in, expense categories for money out.**
  _Source: `StatementCategoryCatalog.isAllowed`._
- Failures → `STATEMENT_CLASSIFIER_FAILED`.

### Logs

- **`grep StatementImport` shows one whole import.** The use case logs `[StatementImport]` with
  `Start`, `Step n/4 … started`, `… done in <ms>`, the classify batches (`batch i/n`), `Done in
  <ms>`, and `Failed at <step> after <ms>`. Each adapter logs `[StatementImport:<Name>]`:
  `Rest`, `PdfBoxUnlock`, `Reader` (steps 1a/1b), `PdfBoxSplit`, `PdfBoxText`, `Azure` (part
  i/n sent/done), `OpenRouter`, `Gemini`, `Ollama` (characters sent, HTTP status and time),
  `LlmParse`, `Jev`, `Async` (started / finished / failed), `LlmCompare` (result paths). A new
  adapter brings its own tag. _Source: `ExtractStatementMovementsInputPort`._
- **Only counts, model names, status codes, error codes and times are logged**, never statement
  content. Keep it that way in new log lines.
- No request id: two uploads at the same time mix their lines.

### Privacy and files

- **The PDF password is never logged and never put in an exception.** Unlock happens in the
  REST layer, so the use case only sees unlocked bytes. _Source: `PdfStatementUnlocker`._
- **No password → `STATEMENT_PASSWORD_REQUIRED`; wrong password → `STATEMENT_WRONG_PASSWORD`;**
  a PDF with only an owner password opens with `""` and is unlocked.
- **Uploads never stay on disk.** `quarkus.http.body.delete-uploaded-files-on-end=true`.
- **The service needs internet access** (Azure, OpenRouter, Gemini, Jev), so in production
  `jbh-private` must keep outbound access. _Source: `jbh-deploy/README.md`._
- **Tests:** unit tests live only in `jbh-finance-application` (no tests in `*-infra`), so the
  provider adapters have no unit tests. _Source: project `CLAUDE.md`._

### Open questions (as of 2026-10-02)

- **Installment purchases:** the rules do not say whether `amount` is the full purchase or this
  period's installment; the LLM chooses. Check the first real credit card import.
- **Card interest and fees may not be movement rows.** The card summary adds "Intereses
  corrientes", "Intereses de mora", "Avances" and "Otros cargos". If they are not rows, the totals
  check shows `MISMATCH` even with correct balances.
- **Jev is weak on Colombian merchants:** on the first real import (2026-10-02), 14 of 33
  movements needed review; the same chain got different answers (Carulla → Unknown, Éxito →
  grocery at low confidence), and Amazon → Unknown at high confidence.
- Run end to end on 2026-10-02 with Azure + OpenRouter + Jev; the lower-case config values
  (`azure`, `openrouter`) map to the enums correctly.
