## 1. Shared ordered executor and the wait (D2, D6)

- [x] 1.1 Change `AsyncTaskExecutorImpl` (`jbh-finance-application/src/main/java/com/jbh/finance/application/async/`): one `ThreadPoolExecutor` per instance (core 1, max 1, unbounded queue, daemon threads, `allowCoreThreadTimeOut(true)`); remove the per-task executor and the `shutdown()/close()` in `finally`. Keep logging, DLQ and metadata behavior. Depends on: none.
- [x] 1.2 Add `void awaitIdle(Duration timeout)` to `AsyncTaskExecutor` and implement it with a no-op sentinel task (timeout, interrupt, worker-thread and failed-task rules in design.md D6; JavaDoc: never call inside a transaction). Depends on: 1.1 (same file).
- [x] 1.3 Append the 6 new methods to `AsyncTaskExecutorImplTest` (design.md, Test contract). Do not edit existing methods. Depends on: none (written against the contract).

## 2. Transaction and lock ports (D3, D5)

- [x] 2.1 `UnitOfWork`: add `executeInTransaction(TransactionalWork<T>)` and the nested `TransactionalWork<T>` functional interface (design.md D5). Depends on: none.
- [x] 2.2 `ProductRepository`, `ProductLifecycleService`, `ProductLifecycleServiceImpl`: add `lockForUpdate(ProductId)`; the service delegates to the repository. Depends on: none.
- [x] 2.3 Test fakes: `UnitOfWorkTest` holds one static `ReentrantLock` for each call and implements the new method; `InMemoryProductRepository.lockForUpdate` checks the product exists (design.md D8). Depends on: 2.1, 2.2.

## 3. Infra adapters (D3, D5; module `jbh-finance-infra`)

- [x] 3.1 `ProductRepositoryAdapter.lockForUpdate`: `@Transactional(TxType.MANDATORY)`, `jpaRepo.findById(productId.value(), LockModeType.PESSIMISTIC_WRITE)`, throw the existing not-found error when missing. Check the Panache API with Context7. Depends on: 2.2.
- [x] 3.2 `QuarkusUnitOfWork`: class annotation `@Transactional(rollbackOn = Exception.class)`; `execute` logs and rethrows; implement `executeInTransaction` (log, rethrow the same exception); keep `executeWithResult` as it is. Remove the two FIXME comments that this fixes. Depends on: 2.1.

## 4. Monthly balance sync (D1, D6)

- [x] 4.1 `ProcessMonthlyBalanceServiceImpl`: add `UnitOfWork` as constructor argument; in `syncForNewMovement`, `syncForReversedMovement` and `persistBalancesAsync`, save on the caller thread and submit only the adjust through one private method; the adjust task runs `unitOfWork.executeInTransaction(...)` with `lockForUpdate(productId)` first, then adjust, then the fully-withdrawn update. Depends on: 1.1, 2.1, 2.2.
- [x] 4.2 `ProcessMonthlyBalanceService`: add `void awaitPendingSync()` (10 s named constant in the impl) and the lock precondition in the JavaDoc of the 3 sync methods. Depends on: 1.2, 4.1 (same files).
- [x] 4.3 Add `ProcessMonthlyBalanceServiceImplTest` with the 5 tests in design.md (Test contract). Depends on: none (written against the contract).

## 5. Flows take the lock (D3, D7, D9)

- [x] 5.1 `ProcessMovementServiceImpl.processMovement`: run the whole body in one `executeInTransaction`; `lockForUpdate(productPK.productId())` is the first statement; the old inner `unitOfWork.execute` block becomes plain code inside it. Depends on: 2.1, 2.2.
- [x] 5.2 `RegisterMonthlyBalanceInputPort`: add `UnitOfWork` and `ProcessMonthlyBalanceService` as constructor arguments; two transactions with `awaitPendingSync()` between them, only after an initial movement (design.md D7); remove the `Thread.sleep(2 s)` block and its FIXME comment. Depends on: 2.1, 2.2, 4.2.
- [x] 5.3 `AddMovementsUploadedFileInputPort.uploadMovementsFromFile`: after `validateUploadedMovements`, run the rest in one `executeInTransaction` with `lockForUpdate(accountId)` first; inline `persistMovementAndAccountUOW` into it (design.md D9). Depends on: 2.1, 2.2, 4.1.
- [x] 5.4 Wiring: `ProductUseCasesCDIConfig` (`jbh-finance-infra`, reuse the injected `unitOfWork` and the `processMonthlyBalanceService()` producer) and `UseCaseFixtureBuilder` (`new UnitOfWorkTest()`). Depends on: 4.1, 5.2.
- [x] 5.5 Add `RegisterMonthlyBalanceInputPortTest` (4 tests), `ProcessMovementServiceImplLockTest` (2 tests) and `AddMovementsUploadedFileInputPortLockTest` (1 test) from design.md (Test contract). Depends on: none (written against the contract).

## 6. Tests stop sleeping (D8)

- [x] 6.1 `UseCaseFixtureBuilder`: one `private static final` shared `ProcessMonthlyBalanceService`; `buildProcessMonthlyBalanceSrv()` returns it; rename `delayTests()` to `awaitBackgroundTasks()` with body `awaitPendingSync()`; `resetState()` calls it first. Depends on: 4.2, 5.4 (same file).
- [x] 6.2 Apply the one-for-one replacement rule (design.md D8) in the 12 test classes and `DeleteMovementUCFixture`. Add one call before the next-month read in `month4ProfitReport202411` and `month6ProfitReport202501` only if none is there. Fix the static imports of `delayTests`. Do not change any assertion. Depends on: 6.1.
- [x] 6.3 Constructor arguments only: `RegisterMovementValidationTest`, `RegisterMovementExecutionTest`, `UploadMultiMovementsExecutionTest` get `new UnitOfWorkTest()` where they build `ProcessMonthlyBalanceServiceImpl` or a changed use case. Depends on: 4.1, 5.2.
- [x] 6.4 Check: `/usr/bin/grep -rn "Thread.sleep\|delayTests" jbh-finance/jbh-finance-application/src/test` lists only `AsyncTaskExecutorImplTest`, and `/usr/bin/grep -rn "Thread.sleep" jbh-finance/jbh-finance-application/src/main` lists nothing. Depends on: 5.2, 6.2.

## 7. Verification (needs groups 1 to 6)

- [x] 7.1 Check the lock rule: every caller of `syncForNewMovement`, `syncForReversedMovement` and `persistBalancesAsync` in `src/main` runs inside a transaction that called `lockForUpdate` first (codegraph callers). List any that do not. Depends on: 5.1, 5.2, 5.3.
- [x] 7.2 Leader or user runs the suites below. Expected: `RegisterMonthlyReportedProfitTest` (all methods, including `month4ProfitReport202411` and `month6ProfitReport202501`), `DeleteMovementUseCaseTest.deleteExpense30WithSnapshot`, `AsyncTaskExecutorImplTest`, `ProcessMonthlyBalanceServiceImplTest`, `RegisterMonthlyBalanceInputPortTest`, `ProcessMovementServiceImplLockTest`, `AddMovementsUploadedFileInputPortLockTest`, `RegisterMovementExecutionTest`, `UploadMultiMovementsExecutionTest` all pass; `jbh-finance-infra` compiles. Depends on: all tasks above.
- [x] 7.3 Run the full module suite 5 times in a row. All 5 runs pass. Report the run time of `RegisterMonthlyReportedProfitTest` before (about 33 s) and after. Depends on: 7.2.
- [ ] 7.4 Manual check by the user (optional, not run by the pipeline): start 2 instances on one database, send two movements for the same product and month at the same time (one to each instance), and check that the month keeps both. Depends on: 7.2.
- [x] 7.5 Automated database lock test (user decision, 2026-10-20): add `ProcessMovementServiceDatabaseLockTest`, a `@QuarkusTest` in `jbh-finance-infra` on a real PostgreSQL from Quarkus Dev Services. Two threads, each in its own transaction, act like two instances. It sends 2 movements for the same product and month at the same time and checks that the month and the product keep both (design.md, Test contract, "New: `ProcessMovementServiceDatabaseLockTest`"). Needs Docker. Depends on: 7.2.
  - a. `jbh-finance-infra/pom.xml`: add `io.quarkus:quarkus-junit5` (test scope, version from the Quarkus BOM); configure surefire with `useModulePath=false`, the system property `java.util.logging.manager=org.jboss.logmanager.LogManager`, and `excludedGroups=${jbh.test.excludedGroups}` with the property `jbh.test.excludedGroups=database` (the test is opt-in; see the open question in design.md).
  - b. Add `FinanceDatabaseTestProfile` (a `QuarkusTestProfile`) with the config overrides listed in the Test contract.
  - c. Add `src/test/resources/db/test-create-finance-schema.sql` with one statement: `CREATE SCHEMA IF NOT EXISTS finance;`.
  - d. Add `ProcessMovementServiceDatabaseLockTest` with the scenario and expected values from the Test contract. Write it against the contract, not against the code.
  - e. Leader or user runs `mvn -pl jbh-finance/jbh-finance-infra -am test -Dtest=ProcessMovementServiceDatabaseLockTest -Dsurefire.failIfNoSpecifiedTests=false -Djbh.test.excludedGroups=none` with Docker running. Expected: 5 of 5 repetitions pass. Then `mvn -pl jbh-finance/jbh-finance-infra -am test` without the flag: the test is skipped and the rest passes.

Suite commands (not run by the worker):

```
mvn -pl jbh-finance/jbh-finance-application test -Dtest='RegisterMonthlyReportedProfitTest,DeleteMovementUseCaseTest,AsyncTaskExecutorImplTest,ProcessMonthlyBalanceServiceImplTest,RegisterMonthlyBalanceInputPortTest,ProcessMovementServiceImplLockTest,AddMovementsUploadedFileInputPortLockTest,RegisterMovementExecutionTest,UploadMultiMovementsExecutionTest'
for i in 1 2 3 4 5; do mvn -q -pl jbh-finance/jbh-finance-application test || break; done
mvn -pl jbh-finance/jbh-finance-infra -am test-compile
```
