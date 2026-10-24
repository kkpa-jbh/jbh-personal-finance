## Why

Ticket JBH-62. The test `RegisterMonthlyReportedProfitTest.registerReportWithRetefuente20252`
fails. It expects movement balance `2.00` in period 2025-03. It gets `-3.00`.

The test is correct. The code has a race. Registering a monthly report adds two movements
to the same month: a dividend deposit (+5) and a retefuente withdrawal (-3). Each movement
reads the month on the caller thread, but the save runs later in a background task. The second
movement reads the month before the first save is done. Both start from an empty month.
The last save wins. The dividend is lost.

A second cause makes it worse. `AsyncTaskExecutorImpl.submitTask` creates a new
single-thread executor for each task. Background tasks run in parallel and in no order.
An older task can overwrite newer data.

The same race can hit any two quick movements in the same month. It is a data-loss bug in
production, not only a test problem.

The app runs as more than one instance. Two requests for the same product on two instances lose
updates the same way. No flow takes a database lock today, so the fix must work in PostgreSQL, not
inside one JVM.

## What Changes

- **Product row lock.** Every flow that changes a product's balances runs in one transaction and
  locks the product row first (`PESSIMISTIC_WRITE`, new port method `lockForUpdate`). Flows: process a
  movement, register an official report, upload movements, and the background adjust step. Writes
  for one product now run one after the other, on all instances.
- **Full rollback.** `UnitOfWork` gets `executeInTransaction`, which allows the checked
  `BusinessException`. `QuarkusUnitOfWork` rolls back on any exception and rethrows it. It no longer
  swallows errors.
- `ProcessMonthlyBalanceServiceImpl.syncForNewMovement`, `syncForReversedMovement` and
  `persistBalancesAsync` save the monthly balance **synchronously**, inside the caller's locked
  transaction. Only the adjust step (opening balance, profit, next months, fully-withdrawn update)
  stays in the background, in its own locked transaction.
- `AsyncTaskExecutorImpl` uses **one shared single-thread executor per instance**. Background
  tasks run in submit order on each instance. The executor no longer shuts down after each task.
- `AsyncTaskExecutor` gets `awaitIdle(Duration)`: wait until every task submitted before the call
  on this instance has finished. `ProcessMonthlyBalanceService` exposes it as `awaitPendingSync()`.
  It is never called inside a transaction.
- `RegisterMonthlyBalanceInputPort` runs in two locked transactions. After the first report's
  initial movement, it commits, calls `awaitPendingSync()` instead of `Thread.sleep(2 s)`, then
  locks again and writes the report.
- Tests stop using sleeps to wait for background work. `UseCaseFixtureBuilder` shares one
  `ProcessMonthlyBalanceService` and `delayTests()` becomes `awaitBackgroundTasks()`. The test
  `UnitOfWorkTest` holds a lock for each call, like the database row lock. About 55 sleeps in 13 test
  files are replaced.
- New regression tests. Existing tests keep their expected values.
- No database change. No schema, no migration. `UNIQUE (product_id, period)` already exists.

## Capabilities

### New Capabilities

- `monthly-balance-sync`: how a new or reversed movement is saved into its monthly balance, and
  how the follow-up adjust step runs in the background.

### Modified Capabilities

None. No spec exists in `openspec/specs/` yet.

## Impact

- Code: `jbh-finance/jbh-finance-application`
  - `ProcessMonthlyBalanceServiceImpl.java`, `ProcessMonthlyBalanceService.java`
  - `AsyncTaskExecutorImpl.java`, `AsyncTaskExecutor.java`
  - `UnitOfWork.java`
  - `ProductRepository.java`, `ProductLifecycleService.java`, `ProductLifecycleServiceImpl.java`
  - `ProcessMovementServiceImpl.java`
  - `RegisterMonthlyBalanceInputPort.java` (two more constructor arguments)
  - `AddMovementsUploadedFileInputPort.java`
- Code: `jbh-finance/jbh-finance-infra`
  - `ProductRepositoryAdapter.java` (the row lock)
  - `QuarkusUnitOfWork.java` (rollback and rethrow)
  - `ProductUseCasesCDIConfig.java` (wiring only)
- Test fakes and fixtures: `UnitOfWorkTest.java`, `InMemoryProductRepository.java`,
  `UseCaseFixtureBuilder.java`, `DeleteMovementUCFixture.java`, the 12 test classes that wait with
  `Thread.sleep` / `delayTests()`, and 3 test classes that only get new constructor arguments.
  The sleeps inside `AsyncTaskExecutorImplTest` stay: they simulate work, they do not wait for it.
- Tests: 4 new test classes and 6 new methods in `AsyncTaskExecutorImplTest`.
- Behavior:
  - Requests for the same product wait for each other, on all instances.
  - Any failure in a balance flow now rolls back and reaches the caller. Before, some errors were
    only logged and half the work stayed saved.
- Tests that should pass after the fix, every run: `RegisterMonthlyReportedProfitTest` (all
  methods, including `month4ProfitReport202411` and `month6ProfitReport202501`, which also failed at
  random) and `DeleteMovementUseCaseTest.deleteExpense30WithSnapshot`.
- Test run time drops: `RegisterMonthlyReportedProfitTest` alone spends most of its 33 s in sleeps.
- Other modules: none. `jbh-finance-infra` still uses `new AsyncTaskExecutorImpl()`.
