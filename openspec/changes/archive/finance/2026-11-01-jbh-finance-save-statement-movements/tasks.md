## 0. Test blueprint (Test-First: tests + skeleton, nothing implemented) (D1 to D25)

Write the unit tests from the Test contract in design.md, not from code. Write the smallest
skeleton that compiles them: only public types and signatures, each new method body throws
`UnsupportedOperationException("not implemented")`. No logic, no database change, no migration.
Do not change an existing signature in a way that breaks a caller; add overloads and components
with compatibility constructors. Each new test must fail because the behavior is missing.

- [x] 0.1 Skeleton, domain (`jbh-finance-domain`): `ProductType.allowsStatementImport()`; `ProcessMovementOptionsVO` gets the component `skipFundsCheck` plus the two existing constructors (skip = false) and `forStatementImport()`; new record `MovementReviewVO(BigDecimal confidence, boolean needsReview)`; `MovementMetadata.putStatementReview(MovementReviewVO)`; `MovementMetadataKey.STATEMENT_CONFIDENCE`, `STATEMENT_NEEDS_REVIEW`; `MovementDomain` gets a nullable `statementImportId` (field, getter, one extra constructor; the existing constructors pass `null`). Depends on: none.
- [x] 0.2 Skeleton, application movement: `AddMovementUploadedFileCommand` gets `category`, `description`, `statementImportId`, `review` (existing 3- and 4-argument constructors keep working, new fields `null`); new `UploadMovementsBatchCommand(movements, options)` with `forFile(list)` and `forStatement(list)` (throw); new overload `uploadMovementsFromFile(UUID, ProductId, UploadMovementsBatchCommand)` on `AddMovementsUploadedFileUseCase` and `AddMovementsUploadedFileInputPort` (throws); `ProductLifecycleService.syncByUploadedMovements(domain, movements, options)` and `ProductLifecycleServiceImpl` (throws); `MovementDTO.statementImportId` (field + accessor); `MovementQueryRepository.findByStatementImportId(UUID)` with the infra adapter and the fake `InMemoryMovementQueryRepository` (throw). Depends on: 0.1.
- [x] 0.3 Skeleton, application statement: `BusinessApplicationExceptionType` gets `STATEMENT_PRODUCT_TYPE_NOT_ALLOWED`, `STATEMENT_ALREADY_IMPORTED`, `STATEMENT_IMPORT_NOT_FOUND`, `STATEMENT_IMPORT_TIMED_OUT`, `STATEMENT_IMPORT_FAILED` (en and es messages); `PdfSha256`; `StatementMainCurrencyPolicy`; `StatementImportRepository` (port); DTOs `StatementImportStatus`, `StatementImportDTO`, `NewStatementImportDTO`, `StatementImportSummaryDTO`, `SkippedStatementMovementDTO` (with the closed enum `SkipReason`: `OTHER_CURRENCY`, `FUTURE_DATE`), `StatementImportResultDTO`, `StatementImportMovementDTO`, `StatementImportSettings`; commands `StartStatementImportCommand`, `RunStatementImportCommand`, `FindStatementImportCommand`; use case interfaces and the three input ports (constructor stores the fields, the run port also takes a `Clock`; methods throw); `StatementCategoryCatalog.fallbackAliasFor(direction)` and `categoryOf(alias)` (throw); `CategorizedMovementDTO.category` and `StatementClassificationDTO.balances` as new components (the producer in `ExtractStatementMovementsInputPort` passes `null` and an empty list for now). Depends on: 0.1, 0.2.
- [x] 0.4 Domain tests: `ProcessMovementOptionsVOTest` (U8), `ProductTypeTest` (U10), `MovementMetadataTest` (U11), and the new cases in `ProductDomainTest` (U9). Depends on: 0.1.
- [x] 0.5 Application tests, movement: `AddMovementUploadedFileCommandTest` (U12), `AddMovementsUploadedFileInputPortStatementTest` (U13); change the stub in `AddMovementsUploadedFileInputPortLockTest` to the 3-argument `syncByUploadedMovements` (no assertion removed). Depends on: 0.2.
- [x] 0.6 Application tests, statement helpers: `PdfSha256Test` (U1), `StatementMainCurrencyPolicyTest` (U2), new cases in `StatementCategoryCatalogTest` (U3), and the updated and new cases in `ExtractStatementMovementsInputPortTest` (U4). Depends on: 0.3.
- [x] 0.7 Application tests, use cases: fake `InMemoryStatementImportRepository` (`testfixtures/fakes/statement/`), `StartStatementImportInputPortTest` (U5), `RunStatementImportInputPortTest` (U6, including the D25 future-date cases), `FindStatementImportInputPortTest` (U7). Depends on: 0.2, 0.3.

## 1. Domain (D5, D9, D11, D6, D8)

- [x] 1.1 `ProductType.allowsStatementImport()`: true for SAVINGS and CREDIT_CARD (use a private static `List`, like `ADDING_MOVEMENTS_PRODUCTS_ALLOWED`). Depends on: 0.4.
- [x] 1.2 `ProcessMovementOptionsVO`: implement `forStatementImport()` (ADD, not month-reported, `skipFundsCheck` true). `ProductDomain.syncBalancesByMovement`: call `validateInsufficientNetFlow` only when `skipFundsCheck` is false. No boolean method parameter. Depends on: 0.4 (same files as 0.1).
- [x] 1.3 `MovementMetadata.putStatementReview` (two keys); `MovementDomain.statementImportId` kept by `MovementMapper` (see 2.4). Depends on: 0.4.

## 2. Application, movement (D5, D11, D23)

- [x] 2.1 `ProductLifecycleServiceImpl`: implement the 3-argument `syncByUploadedMovements`; the 2-argument method delegates with `new ProcessMovementOptionsVO(false, EntityOperationVO.ADD)`. Depends on: 0.5, 1.2.
- [x] 2.2 `UploadMovementsBatchCommand`: `forFile` (default options), `forStatement` (`ProcessMovementOptionsVO.forStatementImport()`); `validate()` rejects null or empty list. Depends on: 0.5, 1.2.
- [x] 2.3 `AddMovementsUploadedFileInputPort`: implement the new overload; the old method calls it with `forFile`. Split `mapCommandToDomain` first into `toMovementDomain` and `resolveCategory` (see Complexity notes in design.md); use the command category when present, else today's default; set description, `statementImportId`, review metadata. Depends on: 2.1, 2.2, 1.3.
- [x] 2.4 `MovementMapper.toDTO` / `toDomain` and `MovementDTO` carry `statementImportId`. Depends on: 0.5, 1.3.

## 3. Application, statement (D2, D3, D4, D6, D10, D15, D16, D20 to D22, D24, D25)

- [x] 3.1 `StatementCategoryCatalog`: load `findIncomeOther`; per-direction fallback and last option; `fallbackAliasFor`, `categoryOf`; `isAllowed(CREDIT, expense Unknown)` is false. Depends on: 0.6.
- [x] 3.2 `ExtractStatementMovementsInputPort`: per-row `categorizeOne` with the Unknown rule (D2, D21); fill `CategorizedMovementDTO.category` and `StatementClassificationDTO.balances`. Depends on: 3.1.
- [x] 3.3 `PdfSha256` and `StatementMainCurrencyPolicy` (D20). Depends on: 0.6.
- [x] 3.4 `StartStatementImportInputPort` (+ use case, command): owner check, `allowsStatementImport`, `markStaleAsFailed`, `insertProcessing` with a new `UUID`, `STATEMENT_ALREADY_IMPORTED` on `false`. Depends on: 0.7, 1.1, 3.3.
- [x] 3.5 `RunStatementImportInputPort` (+ use case, command; takes a `Clock`): extract, main currency, split rows into saved and not saved (`OTHER_CURRENCY`, then `FUTURE_DATE` when the date is after `LocalDate.now(clock)`; skip the upload when nothing is left, D25), sort by date, map rows, summary, one `executeInTransaction` with upload then `markDone` (D16), failure marking, `Optional<StatementClassificationDTO>` result. Depends on: 0.7, 2.3, 2.4, 3.2, 3.3.
- [x] 3.6 `FindStatementImportInputPort` (+ use case, command): owner check, row lookup, effective status with `Clock` and `staleAfter`, movements via `findByStatementImportId`, mapping (direction from type, absolute amount, metadata to confidence and needs-review), stable sort by (date, createdAt). Depends on: 0.7, 2.4.

## 4. Database and persistence, infra (D7, D8, D9, D10, D17)

- [x] 4.1 Liquibase: create `db/changelog/007_create_statement_imports.sql` exactly as in design.md and add the include in `db/finance-db-master.xml`. Depends on: 0.3.
- [x] 4.2 `StatementImportJPAEntity` (`finance.statement_imports`, `summary` as `@JdbcTypeCode(SqlTypes.JSON)` `Map<String, Object>`), `StatementImportJPARepository`, `StatementImportRepositoryAdapter` implementing `StatementImportRepository`: native `INSERT ... ON CONFLICT (product_id, pdf_sha256) WHERE status <> 'FAILED' DO NOTHING`; conditional bulk UPDATEs for stale, done, failed (each returns the row count); every method `@Transactional`. Summary DTO to map with Jackson `convertValue` (BigDecimal and `LocalDate` must keep their value). Check Panache and native query APIs with Context7. Depends on: 3.4 (port + DTOs), 4.1.
- [x] 4.3 `MovementJPAEntity.statementImportId` (plain `UUID` column, no association); `toEntity` and `toDTO` map it; `MovementJPARepository.findByStatementImportId` (join fetch the category); `MovementRepositoryQueryAdapter` implements the new port method. Depends on: 2.4, 4.1.

## 5. REST, async and wiring, infra (D1, D12, D13, D19, D24)

- [x] 5.1 `FinanceApiRoutes`: remove `STATEMENTS_API_PATH` and `STATEMENTS_EXTRACT_API`; add `STATEMENT_IMPORTS_API_PATH = PRODUCTS_API_PATH + "/{productId}/statement-imports"` and `STATEMENT_IMPORT_ID_PATH = "/{importId}"`. Depends on: none.
- [x] 5.2 Responses (`rest/statement/response`): `StatementImportAcceptedResponse`, `StatementImportResponse`, `StatementImportSummaryResponse`, `StatementImportMovementResponse`, `SkippedStatementMovementResponse`, each with `fromDTO`; keep `TotalsCheckResponse`; delete `StatementClassificationResponse` and `CategorizedMovementResponse`. Depends on: 3.6.
- [x] 5.3 `StatementImportRestAdapter` (replaces `StatementRestAdapter`, which is deleted): POST (file checks, hash of the original bytes, unlock, `StartStatementImportUseCase`, dispatch, `202`) and GET. Extend `BaseRestAdapter`, `@Path(STATEMENT_IMPORTS_API_PATH)`, OpenAPI annotations like the old adapter. Depends on: 3.4, 3.6, 5.1, 5.2, 5.4.
- [x] 5.4 `StatementImportDispatcher` and `StatementImportRunner`: carry `RunStatementImportCommand`; the runner calls `RunStatementImportUseCase.run` and passes a present result to `JevReport.write`; keep `@ActivateRequestContext` on the runner. Depends on: 3.5.
- [x] 5.5 `FinanceGlobalExceptionHandler`: `STATEMENT_ALREADY_IMPORTED` -> `409` (D19). Depends on: 0.3.
- [x] 5.6 `StatementUseCasesCDIConfig`: produce `StartStatementImportUseCase`, `RunStatementImportUseCase`, `FindStatementImportUseCase` (inject `ProductLifecycleService`, `StatementImportRepository`, `MovementQueryRepository`, `UnitOfWork`, `AddMovementsUploadedFileUseCase`, `ExtractStatementMovementsUseCase`, `Clock`; the Clock also goes to the run use case); add `StatementImportConfig.staleAfter()` and the property `jbh.statement.stale-after` in `application.properties`; add the new use case classes to `@RegisterForReflection` if the project needs it there. Depends on: 3.4, 3.5, 3.6, 4.2.

## 6. Integration tests (written against the contract in design.md; do not read the implementation)

- [x] 6.1 `StatementImportRestHttpTest` (`@QuarkusTest`, tag `database`): IT1, IT2, IT3, IT4, IT5, IT7, IT9, IT10. Depends on: 5.3, 5.6.
- [x] 6.2 `StatementImportDatabaseTest` (`@QuarkusTest`, tag `database`): IT6, IT8, IT11. Depends on: 4.2, 5.6.

## 7. Docs and verification

- [x] 7.1 Update `jbh-finance/AGENTS.md`: add `statement_imports` to the Tables list and a line about the statement import (admission, run, find) under Responsibility. Depends on: 5.6.
- [x] 7.2 Sync notes (no edit): `../jbh-deploy/README.md` needs no route change (the paths stay under `/jbh-api/finance/**`, no new top-level path, no port or Consul change). After this change, run `kkpa-context-curate` for `kkpa/context/use-case/jbh-finance/extract-statement-movements.md` and `kkpa/context/input-port/jbh-finance/import-statement.md`. Do not edit those two files by hand. Depends on: none.
- [x] 7.3 Leader checks (run by the leader or the user, not by workers): `mvn clean verify` (tests + PMD + SpotBugs), `mvn -q pmd:check` (complexity), and the opt-in database tests: `mvn -pl jbh-finance/jbh-finance-infra -am verify -Djbh.test.excludedGroups=none` (needs Docker). Depends on: all groups.

Dependency summary: group 0 first. Then 1, then 2, then 3 and 4 can run in parallel after 2.4 and 1.x (3.1 to 3.3 need only group 0). Group 5 needs 3 and 4. Group 6 needs 5. Files in parallel tasks are disjoint, except 1.2 / 0.1 and 3.2 / 3.1 / 0.6, which are marked.

Size estimate: 92 files, ~3800 changed lines (tests and docs included)
