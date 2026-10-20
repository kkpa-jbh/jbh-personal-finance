## Context

Registering a monthly report (`ReportedProfitStrategy.registerOfficialMonthlyBalance`) calls
`ProcessMovementServiceImpl.addDividendsMovementForNextMonth`. That adds two movements to the next
month: a dividend deposit and a retefuente withdrawal. Each call ends in
`ProcessMonthlyBalanceServiceImpl.syncForNewMovement`, which does:

1. read the month (`findMonthlyBalanceOfMovement`) on the caller thread,
2. `assignMovement` in memory,
3. `persistBalancesAsync`: the save (`saveMultiBalances`) and then
   `adjustCurrentAndNextMonthlyBalancesAsync` run later in a background task.

`AsyncTaskExecutorImpl.submitTask` creates a new `Executors.newSingleThreadExecutor()` per task.
Tasks run in parallel with no order. `syncForReversedMovement` has the same shape.

Failure: the second movement reads the month before the first background save is done. Both start
from an empty 2025-03. The last save wins. Only -3 stays. Movement balance is -3.00, not 2.00.

**The app runs as more than one instance** (project rule "Horizontal scaling", 2026-10-19). So the
same lost update also happens between two instances, and no in-JVM fix can stop it. Facts checked
on 2026-10-19:

- No flow takes a database lock. There is no `@Version` column and no `FOR UPDATE` in the code.
- The monthly balance save is a blind `merge` of an entity built from a DTO. The last write wins.
- `ProcessMovementServiceImpl.processMovement` reads the product and computes its new balance
  **outside** any transaction. Only "save movement + save product" runs in `unitOfWork.execute`.
  The monthly balance sync runs after that transaction, with no transaction of its own.
- `RegisterMonthlyBalanceInputPort` and `AddMovementsUploadedFileInputPort` read and compute outside
  any transaction. Each repository adapter is `@Transactional` per call.
- The REST layer opens no transaction. Transactions come only from `QuarkusUnitOfWork` and the
  repository adapters.
- `QuarkusUnitOfWork.execute` catches and logs every exception, so the transaction commits anyway.
- `finance.monthly_balances` already has `UNIQUE (product_id, period)`
  (`uk_monthly_balances_product_period`, migration 001). Two rows for the same month fail at the
  database.
- Transfers call `addMovementToProduct` twice, in two separate transactions. No flow changes two
  products in one transaction.

Callers of `persistBalancesAsync`:

- `ProcessMonthlyBalanceServiceImpl.syncForNewMovement` and `syncForReversedMovement`: change (D1).
- `MonthlyBalanceSyncForUploadedMovements.syncForUploadedMovementsAsync`: its code does not change,
  but its caller now runs it inside the locked transaction (D9).
- Tests `RegisterMovementExecutionTest` and `UploadMultiMovementsExecutionTest` call it and read
  `.get()`. The future still returns the adjusted balances, so their assertions stay.

## Goals / Non-Goals

**Goals**

- Two movements for the same product and month both stay, on one instance or on two.
- Every flow that changes a product's balances is correct with 2 or more instances.
- A failure rolls back the whole flow. No half-saved movement, product or month.
- Fix `RegisterMonthlyReportedProfitTest` (`registerReportWithRetefuente20252` and
  `shouldAddDebitsToReportedMonthlyBalance`) and `DeleteMovementUseCaseTest.deleteExpense30WithSnapshot`
  without editing any assertion.
- Make `RegisterMonthlyReportedProfitTest.month4ProfitReport202411` and `month6ProfitReport202501`
  pass every run. They failed at random (2026-10-19 diagnosis: `0.00` instead of `575.00` at line 640),
  because they read the next month before the background task had finished.
- Remove every `Thread.sleep` that waits for our own background work, in `main` and in tests.
  A wait for background work must end when the work ends, not after a fixed time.

**Non-Goals**

- Flows that change a product but not its balances (`UpdateProductInputPort`, delete product). They
  keep last-write-wins. Follow-up ticket.
- A database lock timeout (`lock_timeout`) or a retry when a lock wait is long. PostgreSQL waits
  without a limit by default. Follow-up if waits become a problem.
- Database change. None. No table, column, index, constraint or migration (D10).
- Replacing `YearMonth.now()` in the adjust step (existing FIXME).
- Renaming `AsyncTaskExecutorImplTest.shouldExecuteMultipleTasksConcurrently` (see D2).
- A DLQ or retry for failed adjust tasks (existing FIXME).
- Sleeps that are not a wait for our own background work. They stay:
  `AsyncTaskExecutorImplTest` (the sleep is inside the task and simulates work), and
  `AzureLayoutTextSourceAdapter` / `JevMovementClassifierAdapter` (polling and `Retry-After` of an
  external API).
- An automated test with 2 real instances and PostgreSQL. Infra modules have no test rule, and the
  pipeline never starts services. The user can run a manual check (tasks 7.4).
  A smaller automated check is in scope (user decision, 2026-10-20): one JVM, two threads, a real
  PostgreSQL from Dev Services (task 7.5, Test contract).

## Decisions

### D1: Save synchronously in the caller, adjust in the background

`syncForNewMovement` and `syncForReversedMovement` do: read, assign (or reverse), then
`saveMultiBalances` on the caller thread, inside the caller's transaction (D5). Then they submit
**only** the adjust step to the executor.

The adjust task runs in its **own** transaction and locks the product first (D3). Its body:
`adjustCurrentAndNextMonthlyBalancesAsync(productId, initPeriod, endPeriod)`, then the
fully-withdrawn update (`isFullyWithdrawn` / `updateWhenFullyWithdrawn`). One private method builds
this task. `syncForNewMovement`, `syncForReversedMovement` and `persistBalancesAsync` all use it.

`persistBalancesAsync` changes the same way: it saves on the caller thread and submits only the
adjust. Its future still returns the adjusted balances.

Why: the next movement must read a month that is already saved. And the save must happen while the
caller holds the product lock. A save in a background task runs after the lock is released, so
another instance could change the month in between.

Precondition: the caller of `syncForNewMovement`, `syncForReversedMovement` and
`persistBalancesAsync` holds the product lock in an open transaction. The JavaDoc of the three
methods in `ProcessMonthlyBalanceService` says so.

Rejected: wait for the future inside the sync (`persistBalancesAsync(...).join()`). It would wait for
a task that waits for our own lock (D3), so it would block until the timeout.

Returned value: `syncedMonthlyBalanceDTO` is the same object as before (balance after the
movement, before the adjust). Callers see no change.

### D2: One shared single-thread executor per `AsyncTaskExecutorImpl` instance

`AsyncTaskExecutorImpl` holds one `ThreadPoolExecutor` field: core 1, max 1, unbounded queue, a
daemon thread factory, and `allowCoreThreadTimeOut(true)` with a keep-alive. `submitTask` submits to
it. The `finally { shutdown(); close(); }` block is removed.

This is **not** what keeps data correct. D3 does that, across instances. D2 gives:

- local order: tasks of one instance run one at a time, in submit order, so they wait less on each
  other's database lock;
- the base for the exact wait in D6 (one thread, FIFO);
- a fixed number of threads instead of one new thread per task.

Daemon thread: it does not block JVM shutdown. Core thread timeout: tests create many
`AsyncTaskExecutorImpl` instances. An idle thread ends by itself. Order is kept because there is
still at most one thread.

Rejected: one executor per product (more threads, more code, not asked). A static shared executor
(hides state, couples separate service instances in tests).

`AsyncTaskExecutorImplTest.shouldExecuteMultipleTasksConcurrently` asserts only each result and
`isDone` with a 5 s timeout. Three tasks of 50 ms run serial in about 150 ms and give the same
results. It still passes. The name is now untrue, but the project rule is append-only, so it
stays. Other tests in that class (interrupt, null metadata, exception metadata) do not depend
on a thread per task. The interrupt test: the worker thread is interrupted by the task; the pool
clears the flag before the next task, so later tasks are not affected.

### D3: The product row lock serializes balance writes across instances

New port method `ProductRepository.lockForUpdate(ProductId)`, exposed as
`ProductLifecycleService.lockForUpdate(ProductId)`. The adapter loads the product row with
`jpaRepo.findById(productId.value(), LockModeType.PESSIMISTIC_WRITE)`. PostgreSQL holds the row lock
until the transaction ends. A second transaction that locks the same product waits.

The adapter method is `@Transactional(TxType.MANDATORY)`. Called without an open transaction, it
fails at once. Without this, the adapter would open its own short transaction and release the lock
at once. A missing product throws the same not-found error as `findOrThrowByIdProductId`.

**One rule, everywhere:** every flow that changes a product's balances opens one transaction and
calls `lockForUpdate(productId)` as its **first** statement. Then it reads, computes and saves.

| Flow | Transaction | Lock |
|---|---|---|
| `ProcessMovementServiceImpl.processMovement` (add, reverse, transfer side, liquidate, dividends) | one, the whole method | first statement |
| Background adjust task (D1) | one per task | first statement |
| `RegisterMonthlyBalanceInputPort.registerOfficialMonthlyBalance` | two (D7) | first statement of each |
| `AddMovementsUploadedFileInputPort.uploadMovementsFromFile` | one, after the command validation (D9) | first statement |

Nested calls join the outer transaction (`REQUIRED`). A second `lockForUpdate` on the same row in the
same transaction returns at once.

Why the product row: every balance write is for one product. One row per product is the smallest
lock that covers the product balance and all its months. Locking month rows does not work: the
adjust step creates months that do not exist yet.

Deadlock: no transaction locks two products (transfers use two transactions). A transaction never
waits for a background task (D6 rule). So no deadlock. If a later flow must lock two products, it
locks them in ascending `ProductId` order. PostgreSQL also detects a deadlock and fails one side.

Rejected: an in-JVM lock (the old D3; it protects one instance only, which breaks the scaling rule).
An optimistic `@Version` column (needs a migration and retry code in every writer). A PostgreSQL
advisory lock (needs native SQL and a key mapping; the row lock does the same with JPA only).

### D4: SOLID scope

SRP and DIP (user decision, 2026-10-19). DIP holds:

- The application layer asks for the lock through its own port, `ProductRepository.lockForUpdate`.
  `LockModeType` and `@Transactional` stay in infra.
- It asks for transactions through its own `UnitOfWork` port (D5).
- The wait (D6) is a method on the existing `AsyncTaskExecutor` port.

The application layer keeps zero framework dependencies. No new interface. One new functional
interface nested in `UnitOfWork` (D5).

### D5: One transaction per flow, and it rolls back on any error

`UnitOfWork` gets one method:

```java
<T> T executeInTransaction(TransactionalWork<T> work) throws BusinessException;

@FunctionalInterface
interface TransactionalWork<T> {
  T run() throws BusinessException;
}
```

It is needed because the flows throw the checked `BusinessException`, which `Runnable` and `Supplier`
cannot. The exception reaches the caller unchanged, so REST error mapping stays as it is.

`QuarkusUnitOfWork` (the known gap, now in scope by user decision 2026-10-19):

- `execute` logs, then rethrows. It no longer swallows the error.
- `executeWithResult` keeps its current behavior (log, wrap in `IllegalStateException`, throw).
- `executeInTransaction` logs, then rethrows the same exception.
- The class annotation becomes `@Transactional(rollbackOn = Exception.class)`. A checked
  `BusinessException` then also rolls back. By default JTA commits on a checked exception.

Result: a failure anywhere in the flow rolls back the movement, the product and the month together.

Behavior change: callers of `execute` that hid an error before now get it. Today this is the
movement flow and the upload flow. The error now reaches the REST layer.

### D6: Wait for background work with a sentinel, not a sleep

`AsyncTaskExecutor` gets one method: `void awaitIdle(Duration timeout)`. `AsyncTaskExecutorImpl`
submits a no-op sentinel task to its one executor and waits for the sentinel's future, with the timeout.

Why it is exact: D2 gives one thread and submit order. When the sentinel has run, every task
submitted before it on this instance has finished. If nothing is pending, the sentinel runs at once.

Rules:

- Timeout: throw `IllegalStateException` with the timeout in the message.
- Interrupted while waiting: restore the interrupt flag, then throw `IllegalStateException`.
- Called from the executor's own worker thread: throw `IllegalStateException` at once. Detect the
  worker with a `ThreadLocal` flag set by the task wrapper (or the thread factory).
- A failed earlier task does not fail the wait. The wait is about "finished", not "succeeded".
- **Never call it inside a transaction.** The background task locks the product (D3). If the caller
  still holds that lock, the task waits for the caller and the caller waits for the task until the
  timeout. The JavaDoc says so, and the register test checks it (Test contract).

`ProcessMonthlyBalanceService` gets `void awaitPendingSync()`. `ProcessMonthlyBalanceServiceImpl`
calls `asyncTaskExecutor.awaitIdle(...)` with a named constant of 10 seconds.

Scaling: this wait is not a data guard. It only lets one request see the result of the background
work that **this same request** started on **this** instance (D7), and lets tests wait. Data safety
across instances comes from D3.

Rejected: return the future from `syncForNewMovement` (changes the return type through
`ProcessMovementService` and `AddMovementResultDTO`, and the REST response). `Thread.sleep` (slow
when idle, too short when the machine is busy).

### D7: Register runs in two transactions and waits between them

`RegisterMonthlyBalanceInputPort` gets two more constructor arguments: `UnitOfWork` and
`ProcessMonthlyBalanceService` (6 in total). `registerOfficialMonthlyBalance` does:

1. `addMonthlyBalanceCommand.validate()` and `validatePeriod(...)`, as today (no database).
2. **Transaction 1:** `lockForUpdate(accountId)`, `validateConsecutiveMonthlyBalances(...)`, read the
   month. If the month does not exist, add the initial balance movement (`addMovementToProduct`).
   Commit.
3. Only if the initial movement was added: `awaitPendingSync()`, **outside** any transaction. This
   replaces `Thread.sleep(2 s)` and its `FIXME TODO - This is a hack` comment.
4. **Transaction 2:** `lockForUpdate(accountId)`, `validateConsecutiveMonthlyBalances(...)` again,
   read the month, throw if it is missing, run the profit strategy (official report and dividend
   movements). Commit.

When the month already exists, step 3 is skipped. Transaction 1 then only validates and reads.

Why two transactions: the official report must be written after the initial movement's adjust has
run. Today the 2 s sleep gives that order. The adjust needs the product lock, so it cannot run while
the register holds it. Commit, wait for our own adjust, then lock again.

Why the wait is safe with 2 instances: it waits only for the adjust this request started, on this
instance. Work from other instances is handled by the lock: it runs fully before transaction 2, or
after it, and in both cases it reads fresh data.

Between the two transactions another request can change the product. Transaction 2 validates again
and reads the month again under the lock, so it never uses data from transaction 1.

The constructor goes from 4 to 6 arguments. The project's 3-parameter rule is for methods; the
constructors here already take 4. Wiring changes: `ProductUseCasesCDIConfig` (infra) and
`UseCaseFixtureBuilder.buildRegisterMonthlyBalanceUseCase`.

Rejected: one transaction with the wait inside (it blocks until the timeout, see D6). Running the
initial movement's adjust on the caller thread (a second adjust path to keep in step).

### D8: Tests wait with `awaitBackgroundTasks()`, and the test unit of work acts like the lock

`UseCaseFixtureBuilder` holds one `private static final ProcessMonthlyBalanceService`.
`buildProcessMonthlyBalanceSrv()` returns it. This is the same shape as production: one
`@ApplicationScoped` bean, so one executor (D2).

`UnitOfWorkTest` gets one `private static final ReentrantLock`. Each `execute`, `executeWithResult`
and `executeInTransaction` holds it for the whole call. This copies what the product row lock does in
PostgreSQL: a background adjust waits until the caller's transaction ends. It is coarser (one lock
for all products), which is fine for tests. Reentrant, so nested units of work work.
`InMemoryProductRepository.lockForUpdate` only checks that the product exists.

This in-JVM lock lives in test code only. It stands in for the database, which the unit tests do not
have. It does not break the scaling rule.

`delayTests()` is renamed to `awaitBackgroundTasks()`. Its body becomes
`SHARED_SERVICE.awaitPendingSync()`. `resetState()` calls `awaitBackgroundTasks()` first, so a task
left by one test class cannot write into the next class after the storage is cleared.

Mechanical rule for the cleanup (one rule, no judgment): every `Thread.sleep(...)` or `delayTests()`
in a test that waits for background work is replaced one-for-one by `awaitBackgroundTasks()`.
A call with nothing pending returns at once, so extra calls cost nothing. Do not delete calls and do not
move them. Files: `DeleteMovementsFromPreviousMonthsUseCaseTest`, `DeleteMovementUseCaseTest`,
`RegisterMonthlyReportedProfitTest`, `RegisterTCMovementTest`, `RegisterInvesmentMovementTest`,
`RegisterCDTMovementTest`, `AddTransferProductsTest`, `RegisterMonthlyReportedWithoutProfitTest`,
`DeleteMovementUCFixture`, `CreateProductTest`, `AddTransferRealEstateTest`,
`AddMovementsAfterMonthlyReportedTest`, and `UseCaseFixtureBuilder` itself.
After the cleanup, `/usr/bin/grep -rn "Thread.sleep\|delayTests" jbh-finance/jbh-finance-application/src/test`
must list only `AsyncTaskExecutorImplTest`.

Tests that build their own `ProcessMonthlyBalanceServiceImpl` or use case (`RegisterMovementValidationTest`,
`RegisterMovementExecutionTest`, `UploadMultiMovementsExecutionTest`) get the new constructor
arguments (`new UnitOfWorkTest()`). Nothing else changes in them.

The in-memory fakes keep their `HashMap`. The test lock serializes the caller and the adjust, and
tests read only after `awaitBackgroundTasks()`.

AI-efficiency: one lock rule ("lock first, in one transaction"), one helper name, one replacement
rule and one grep check. A worker applies them with no per-file judgment.

### D9: Upload runs in one locked transaction

`AddMovementsUploadedFileInputPort.uploadMovementsFromFile`: after `validateUploadedMovements`, the
rest runs in one `executeInTransaction`: `lockForUpdate(accountId)`, find the product, map, sync the
product balance, save movements and product, then `syncForUploadedMovementsAsync`.
`persistMovementAndAccountUOW` no longer opens its own unit of work. The nested one would join anyway,
so it is inlined into the outer one.

`syncForUploadedMovementsAsync` reads the months, assigns the movements, and calls
`persistBalancesAsync`, which now saves on the caller thread (D1). All of it runs under the lock.

### D10: No database change

No schema, index or migration. `UNIQUE (product_id, period)` already exists
(`uk_monthly_balances_product_period`). The lock uses the existing primary key of `finance.products`.
The `database` design gate is not touched.

## Two instances: what happens

Instance A and instance B, same product P.

| Case | Result |
|---|---|
| A adds +5 to March, B adds -3 to March, at the same time | One locks P first. The other waits, then reads the month with +5 already saved. Both movements stay. |
| A's background adjust for March runs while B adds a movement | Whoever locks P first runs fully. The other then reads fresh data. No old copy overwrites new data. |
| A registers the report for February, B adds a dividend to March | Each holds the lock for its whole transaction. They run one after the other. |
| A and B both create the April placeholder | Cannot happen under the lock. If a future flow skips the lock, the unique constraint fails one side and its transaction rolls back (D5). |
| A crashes while holding the lock | PostgreSQL ends the session and releases the lock. The transaction rolls back. |
| A's adjust fails | Its transaction rolls back. The month keeps the saved balance, not adjusted until the next movement. Same as today. |

## Responsibilities

- `ProcessMonthlyBalanceServiceImpl` (changed): syncs the monthly balance with a new or reversed
  movement, saves it, then starts the background adjust in its own locked transaction. Changes when:
  monthly balance sync rules change.
- `ProcessMonthlyBalanceService` (changed): the contract for monthly balance sync. Gets
  `awaitPendingSync` and the lock precondition in its JavaDoc. Changes when: the sync contract changes.
- `ProcessMovementServiceImpl` (changed): processes one movement for one product. Now runs it in one
  locked transaction. Changes when: movement processing rules change.
- `RegisterMonthlyBalanceInputPort` (changed): registers an official monthly report, in two locked
  transactions with a wait between. Changes when: the report registration rules change.
- `AddMovementsUploadedFileInputPort` (changed): imports movements from a file, in one locked
  transaction. Changes when: the import rules change.
- `ProductRepository`, `ProductLifecycleService`, `ProductLifecycleServiceImpl` (changed): get
  `lockForUpdate`. Same job: access to the product. Changes when: product access changes.
- `UnitOfWork` (changed): the port for transactions. Gets `executeInTransaction`. Changes when: the
  transaction contract changes.
- `AsyncTaskExecutorImpl` (changed): runs background tasks in order on one shared thread, and lets a
  caller wait until earlier tasks are done (D6). Changes when: the async execution model changes.
- `AsyncTaskExecutor` (changed): the port for background tasks. Gets `awaitIdle`. Changes when: the
  async contract changes.
- `ProductRepositoryAdapter` (changed, infra): product persistence. Gets the `PESSIMISTIC_WRITE` read.
  Changes when: product persistence changes.
- `QuarkusUnitOfWork` (changed, infra): runs work in one JTA transaction, rolls back and rethrows on
  any error. Changes when: transaction handling changes.
- `ProductUseCasesCDIConfig` (changed, infra): wiring only. Changes when: a constructor changes.
- `UnitOfWorkTest`, `InMemoryProductRepository` (changed, test fakes): act like the transaction and the
  row lock. Changes when: the ports change.
- `UseCaseFixtureBuilder` (changed, test): builds use cases over in-memory fakes, and gives tests one
  way to wait for background work (D8). Changes when: a use case constructor or the wait changes.
- 12 test classes plus `DeleteMovementUCFixture` (changed): only the waits change (D8).
- `RegisterMovementValidationTest`, `RegisterMovementExecutionTest`, `UploadMultiMovementsExecutionTest`
  (changed): constructor arguments only.
- New test classes: `ProcessMonthlyBalanceServiceImplTest`, `RegisterMonthlyBalanceInputPortTest`,
  `ProcessMovementServiceImplLockTest`, `AddMovementsUploadedFileInputPortLockTest`.
- `AsyncTaskExecutorImplTest` (new methods): proves submit order and the wait.
- New production classes: none.
- `ProcessMovementServiceDatabaseLockTest` (new, infra test): proves on a real PostgreSQL that two
  movements for the same product and month, sent at the same time, both stay. Changes when: the lock
  rule or the movement flow contract changes.
- `FinanceDatabaseTestProfile` (new, infra test): boots `jbh-finance-infra` on a throwaway
  PostgreSQL from Dev Services. Changes when: the finance datasource or Liquibase setup changes.
- `test-create-finance-schema.sql` (new, infra test resource): prepares the Dev Services database
  like a real one: the `finance` schema, and `search_path` set to `finance` for the app user.
  Changes when: the module schema name changes.
- `jbh-finance-infra/pom.xml` (changed): test dependency and surefire setup for the opt-in database
  test. Changes when: the module test setup changes.

## Test contract

Written before the implementation. Tests are offline unit tests in `jbh-finance-application`
(in-memory fakes and Mockito). Existing expected values are **not** changed.

### New: `ProcessMonthlyBalanceServiceImplTest`

Package `com.jbh.finance.test.application.feature.monthlybalance.services`. Uses the in-memory
monthly balance fakes, `ProductLifecycleService` from `UseCaseFixtureBuilder`, `UnitOfWorkTest`, and a
fresh product and a fresh `ProcessMonthlyBalanceServiceImpl` per test. Period: 2025-03 (in the past,
so the adjust step runs). Money values use `withJBHDecimals`. Each sync call runs inside
`unitOfWork.executeInTransaction(...)`, like a real caller (D1 precondition).

| Test | Input | Expected |
|---|---|---|
| `shouldKeepBothMovementsWhenTwoMovementsAreSyncedInTheSameMonth` | real `AsyncTaskExecutorImpl`; sync deposit +5 on 2025-03-01, then at once withdrawal -3 on 2025-03-01; then `awaitPendingSync()` | stored 2025-03: totalDebits 5.00, totalCredits 3.00, movementBalance 2.00, totalMovements 2 |
| `shouldSaveMonthlyBalanceBeforeReturningWhenExecutorNeverRuns` | executor stub that drops every task; one sync deposit +5 | stored 2025-03 already has totalDebits 5.00, totalMovements 1; the stub received exactly 1 task |
| `shouldRemoveReversedMovementFromStoredMonthWhenReversalIsSynced` | stub executor that drops tasks; sync deposit +10, sync withdrawal -4, then `syncForReversedMovement` of the withdrawal | stored 2025-03: totalDebits 10.00, totalCredits 0.00, movementBalance 10.00, totalMovements 1 |
| `shouldPropagateExceptionWhenSaveFails` | `MonthlyBalanceLifecycleService` mock whose `saveMultiBalances` throws `IllegalStateException`; stub executor | exception reaches the caller; stub received 0 tasks |
| `shouldLockProductBeforeReadingWhenAdjustRuns` | executor stub that runs the task on the calling thread; Mockito `ProductLifecycleService` and lifecycle service; Mockito `UnitOfWork` that runs the work | `InOrder`: `unitOfWork.executeInTransaction(...)`, then `lockForUpdate(productId)`, then `findNextBalancesFromPeriodInclusive(...)` |

The stub is a small implementation of the existing `AsyncTaskExecutor` that counts calls and
returns a never-completed future (or runs the task, for the last test).

### New methods in `AsyncTaskExecutorImplTest` (append only)

| Test | Input | Expected |
|---|---|---|
| `shouldRunTasksInSubmitOrder` | 3 tasks on one instance; task 0 sleeps 100 ms, task 1 10 ms, task 2 0 ms; each appends its number to a synchronized list | after all futures complete, the list is `[0, 1, 2]` |
| `shouldWaitForEarlierTasksWhenAwaitIdle` | submit a task that sleeps 200 ms, then sets an `AtomicBoolean`; call `awaitIdle(5 s)` | returns; the flag is true |
| `shouldReturnAtOnceWhenAwaitIdleAndNothingPending` | fresh instance; `awaitIdle(1 s)` | returns with no exception |
| `shouldThrowWhenAwaitIdleTimesOut` | submit a task that blocks on a `CountDownLatch`; `awaitIdle(50 ms)` | throws `IllegalStateException`; then release the latch |
| `shouldThrowWhenAwaitIdleIsCalledFromWorkerThread` | submit a task whose body calls `awaitIdle(1 s)` | the task's future completes exceptionally with `IllegalStateException` (no hang) |
| `shouldNotFailAwaitIdleWhenEarlierTaskFailed` | submit a task that throws; `awaitIdle(5 s)` | returns with no exception |

### New: `RegisterMonthlyBalanceInputPortTest`

Package `com.jbh.finance.test.application.feature.monthlybalance.ports.input`. Mockito mocks for the
constructor arguments, except `UnitOfWork`: a small fake that runs the work and keeps an
`inTransaction` flag. `CategoryService.findIncomeInitialBalance()` returns a fixture category.

| Test | Input | Expected |
|---|---|---|
| `shouldLockProductBeforeReadingMonthWhenRegistering` | month exists; command with no profit | `InOrder`: `lockForUpdate(accountId)`, then `findByAccountIdYearAndMonth(...)` |
| `shouldNotAwaitPendingSyncWhenMonthExists` | month exists | `awaitPendingSync()` never called |
| `shouldAwaitPendingSyncBetweenTransactionsWhenFirstReport` | first read empty, second read returns the month | `InOrder`: `addMovementToProduct(...)`, `awaitPendingSync()`, `lockForUpdate(...)`, second read; `awaitPendingSync()` called once; when it is called, the fake's `inTransaction` flag is false |
| `shouldNotSleepWhenFirstReport` | same as above | `assertTimeout(Duration.ofMillis(500), ...)` passes (the old code slept 2 s) |

### New: `ProcessMovementServiceImplLockTest`

Package `com.jbh.finance.test.application.feature.movement.services`. Mockito mocks for the
collaborators; `UnitOfWork` is the same kind of flag fake.

| Test | Input | Expected |
|---|---|---|
| `shouldLockProductFirstWhenAddingMovement` | `addMovementToProduct` with a valid deposit command | `InOrder`: `lockForUpdate(productId)`, `validateNewMovementForOfficialMonthlyReport(...)`, `syncByMovement(...)`, `syncForNewMovement(...)`; the fake's flag is true at `lockForUpdate` and at `syncForNewMovement` |
| `shouldPropagateBusinessExceptionWhenValidationFails` | `validateNewMovementForOfficialMonthlyReport` throws `BusinessException` | the same exception reaches the caller; `syncForNewMovement` never called |

### New: `AddMovementsUploadedFileInputPortLockTest`

Package `com.jbh.finance.test.application.feature.movement.ports.input`. Same fake.

| Test | Input | Expected |
|---|---|---|
| `shouldLockProductFirstWhenUploadingMovements` | 2 valid uploaded deposits | `InOrder`: `lockForUpdate(accountId)`, `findOrThrowByUserAndProductId(...)`, `syncByUploadedMovements(...)`, `syncForUploadedMovementsAsync(...)`; all inside one `executeInTransaction` call |

### Existing tests (expected values unchanged)

1. `RegisterMonthlyReportedProfitTest.registerReportWithRetefuente20252` (order 7). Registering
   2025-02 (closing 582, profit 5, retefuente 3) must create, in 2025-03, a dividend deposit +5 and a
   retefuente withdrawal -3. Expected 2025-03 after `awaitBackgroundTasks()` (was a 1 s sleep): openingBalance 580.00,
   closingBalance 582.00, totalDebits 5.00, totalCredits 3.00, movementBalance 2.00,
   totalMovements 2, officialMonthlyReport false. Product: `movementBalance` = previous + profit
   (5) - retefuente (3), `netProfitBalance` 262.00, `currentBalance` = command closing balance. Why it passes
   now: both movements are saved before the next one is read (D1); the adjusts run after the
   register transaction and read fresh data (D2, D3, D8).
2. `RegisterMonthlyReportedProfitTest.shouldAddDebitsToReportedMonthlyBalance` (order 18). Expected
   product `currentBalance` and `movementBalance` equal `FIRST_BALANCE_ZERO`, i.e. 582 in the
   shared static `finalAccountBalance`. It failed with 575 only because test 7 stopped before
   `refreshActualAccountBalance()`. No code change for it. It passes when test 7 passes.
3. `DeleteMovementUseCaseTest.deleteExpense30WithSnapshot` (order 21). Expected product
   currentBalance 160.00, netProfitBalance 10.00, netGrowthRate 6.67, and current month
   `monthlyNetProfit` 10.00 (line 327; the old run gave 110). Closing balance equals 160.00.
   The movement is gone from the repository.
4. `RegisterMonthlyReportedProfitTest.month4ProfitReport202411` and `month6ProfitReport202501`.
   Assertions unchanged. They read the next month right after register. With D8 the `setUp` and
   in-test sleeps become `awaitBackgroundTasks()`. The step before each "next month" read must be an
   `awaitBackgroundTasks()` call. If the one-for-one rule leaves no call there, add exactly one
   before the read (lines ~497 and ~630 today). This is the only place where a call is added, not replaced.
5. Every class in the module passes **5 runs in a row** (flaky failures were 1 in 3 before).

#### Check: can `deleteExpense30WithSnapshot` still read before the adjust ends?

`monthlyNetProfit` is set only by the adjust step (`recalculateBalances` in the adjust
loop). The test calls `deleteMovementUseCase.deleteMovement(...)` and then reads the monthly
balances through `getCurrentMonthlyBalances()`, which waits first. D8 makes that wait
`awaitBackgroundTasks()`, which ends exactly when the adjust ends. The old value 110 came from
out-of-order tasks writing over newer data. With D1, D2 and the test lock (D8), each task runs in
order and reads fresh data. Assertions stay as they are.

### New: `ProcessMovementServiceDatabaseLockTest` (real PostgreSQL, task 7.5)

The one exception to "offline unit tests": the user asked for it (2026-10-20). The unit tests above
prove the order of calls with fakes. Only a real database proves that `PESSIMISTIC_WRITE` really
makes the second transaction wait. The lock lives in PostgreSQL, so two threads in one JVM, each in
its own transaction, act like two instances.

**Location.** Module `jbh-finance-infra`, file
`jbh-finance/jbh-finance-infra/src/test/java/com/jbh/finance/test/infra/movement/ProcessMovementServiceDatabaseLockTest.java`.
Why this module and not `jbh-z-assembly`:

- It holds everything the flow needs: the producers (`ProductUseCasesCDIConfig`),
  `QuarkusUnitOfWork`, the JPA adapters with the lock, the entities and the Liquibase changelog.
- It has only one datasource (`finance`), so Dev Services start one container. The assembly also
  boots `preferences`, `notifications`, the default datasource, the mailer and Consul registration.
- It is inside the module that owns the change.

**Setup.**

- Class annotations: `@QuarkusTest`, `@TestProfile(FinanceDatabaseTestProfile.class)`,
  `@Tag("database")`.
- `FinanceDatabaseTestProfile` (same package) implements `QuarkusTestProfile`. `getConfigOverrides()`
  returns:

| Key | Value | Why |
|---|---|---|
| `quarkus.datasource.finance.jdbc.url` | empty string | An empty value removes the URL from `application.properties`. Dev Services start only when no URL is set. |
| `quarkus.datasource.finance.devservices.username` / `.password` | `postgres` / `postgres` | Same values as the configured datasource user, so container and pool always match. |
| `quarkus.datasource.finance.devservices.init-script-path` | `db/test-create-finance-schema.sql` | The `finance` schema is created outside Liquibase in real setups (`jbh-deploy/initdb`). Liquibase needs it for its own tables. The script also sets the default schema (see below). |
| `quarkus.hibernate-orm.finance.database.generation` | `none` | Liquibase owns the schema, as in dev and prod. |
| `quarkus.datasource.devservices.enabled` | `false` | The module uses only `finance`. This stops a second, unused container for the default datasource. |
| `quarkus.http.test-port` | `0` | A random free port. The test makes no HTTP call. |
| `quarkus.arc.remove-unused-beans` | `false` | Same as the assembly at runtime. |
| `quarkus.index-dependency.finance-domain.*` / `finance-application.*` | `com.jbh` / `jbh-finance-domain`, `jbh-finance-application` | Same index setup as the assembly. Keeps domain and application free of Jandex. |

- `src/test/resources/db/test-create-finance-schema.sql`, two statements:
  - `CREATE SCHEMA IF NOT EXISTS finance;`
  - `ALTER ROLE CURRENT_USER SET search_path TO finance, public;`
  The default schema **is** needed. Production sets `?currentSchema=finance` in the JDBC URL. The
  Dev Services URL does not. Tables and SQL are schema-qualified, but `MonthlyBalanceJPAEntity`
  names `monthly_balances_seq` with no schema, so without it Hibernate fails with
  `relation "monthly_balances_seq" does not exist`. The script runs at container start, as the same
  user the pool uses, before the pool opens. So every app connection starts in `finance`.
  `CURRENT_USER` works whatever user name Dev Services picked. A JDBC property override
  (`jdbc.additional-jdbc-properties.currentSchema`) was tried and did not reach the connection.
- `quarkus.liquibase.finance.migrate-at-start=true` stays as it is. It creates the tables and the
  system categories that the `ProcessMovementServiceImpl` constructor reads.
- Not started or mocked, because nothing calls them: the gateway client (no REST call, so no
  `findUserId`), Azure, OpenRouter, Gemini and Jev (their keys are already optional). Consul and Gmail
  are not on this module's classpath.
- No REST and no JWT. The test calls the CDI beans directly.
- Injected beans: `ProcessMovementService`, `ProcessMonthlyBalanceService` (the same
  `@ApplicationScoped` instance the movement service uses, so `awaitPendingSync()` waits on the right
  executor), `CreateProductInputPort`, `CategoryService`, `ProductLifecycleService`,
  `MovementLifecycleService`, `UnitOfWork`.
- `@RepeatedTest(5)`. Each repetition makes a new user and a new product, so no cleanup is needed.
  The container lives for the whole test run.

**Scenario (each repetition).**

1. `userId = UUID.randomUUID()`. Create the product with
   `createProductInputPort.execute(new CreateProductCommand(userId, "lock-test", SAVINGS, ProductMetadata.empty()))`.
   SAVINGS updates monthly balances. `productPK = new ProductPK(userId, product.id())`.
2. Seed, one call, no race: `addMovementToProduct(productPK, ...)` with a DEPOSIT of `100` on
   2025-02-01, category `categoryService.findIncomeOther()`. Then `awaitPendingSync()`. The seed puts
   money in the product before the race. Without it, a withdrawal that runs first on a zero balance
   marks the product "fully withdrawn", and the product values would depend on thread order.
3. The race. Two commands for 2025-03-01, the same product:
   - thread A: DEPOSIT `5`, category `findIncomeOther()`;
   - thread B: WITHDRAWAL `3`, category `findExpenseUnknown()`.
   The values match `shouldKeepBothMovementsWhenTwoMovementsAreSyncedInTheSameMonth`.
4. Start gate. A test-only `ExecutorService` with 2 threads. A `CountDownLatch ready = new
   CountDownLatch(2)` and a `CountDownLatch start = new CountDownLatch(1)`. Each task calls
   `ready.countDown()`, then `start.await()`, then `addMovementToProduct(...)`. The main thread calls
   `ready.await(10, SECONDS)`, then `start.countDown()`. So both calls start at the same moment,
   each in its own transaction (`executeInTransaction` on its own thread).
5. Wait. `futureA.get(30, SECONDS)` and `futureB.get(30, SECONDS)`. A failed call fails the test with
   its cause. Then `processMonthlyBalanceService.awaitPendingSync()`, on the main thread, outside any
   transaction. Both adjust tasks were submitted before the calls returned, so the wait ends exactly
   when both adjusts are done (D6). Shut the executor down in `finally`.
6. Read inside `unitOfWork.executeInTransaction(...)`: the 2025-03 month
   (`processMonthlyBalanceService.findByAccountIdAndPeriod(productId, 2025-03)`), the product
   (`productLifecycleService.findOrThrowByIdProductId(productId)`) and the movements
   (`movementLifecycleService.findByAccountId(productId)`).

**Expected values** (money with `withJBHDecimals`):

| What | Expected |
|---|---|
| 2025-03 `totalDebits` | 5.00 |
| 2025-03 `totalCredits` | 3.00 |
| 2025-03 `movementBalance` | 2.00 |
| 2025-03 `totalMovements` | 2 |
| product `currentBalance` | 102.00 (100 + 5 - 3) |
| product `movementBalance` | 102.00 |
| movements of the product | 3 rows; amounts 100.00, 5.00, -3.00 in any order |
| both futures | complete with no exception |

Thread order is not fixed. Every value above is the same for both orders.

**If the lock were missing.** Both transactions read the product (balance 100) and the 2025-03 row
before either commits. Each saves its own copy with a blind `merge`, and the last write wins. The
movement inserts do not conflict, so the test would show: 3 movement rows, but 2025-03 with
`totalMovements` 1 (only 5.00 or only -3.00), and product `currentBalance` 105.00 or 97.00. The month
and product assertions fail. A race does not lose an update on every run, so the test runs 5
repetitions to make a miss very unlikely. With the lock, the second transaction blocks on
`SELECT ... FOR UPDATE` until the first commits, then reads the fresh row. All 5 repetitions pass.

**Commands** (the leader or the user runs them; Docker must run):

```
mvn -pl jbh-finance/jbh-finance-infra -am test -Dtest=ProcessMovementServiceDatabaseLockTest -Dsurefire.failIfNoSpecifiedTests=false -Djbh.test.excludedGroups=none
```

**Open question for the user: opt-in or always on.** Default in task 7.5: opt-in. The pom excludes
the tag `database` (`jbh.test.excludedGroups=database`), so `mvn install` and CI do not need Docker.
The other option: always on. Then every build of `jbh-finance-infra`, CI included, needs Docker and
starts a PostgreSQL container (about 5 to 15 s more per build).

## Risks / Trade-offs

- Requests for the **same product** now wait for each other, on all instances. A long upload holds
  the lock for its whole transaction (seconds for a big file), and other requests for that product
  wait. Requests for other products do not wait. Accepted: correctness over speed for one product.
- A background adjust also holds the product lock while it runs. A slow adjust of old periods delays
  the next request for that product.
- One worker thread per instance (D2). If its task waits for a lock held by another instance, later
  tasks on this instance wait too, also for other products. Accepted with D2.
- The adjust task is submitted inside the caller's transaction. It waits on the lock until the caller
  commits. If the caller rolls back, the adjust still runs and recalculates from committed data, which
  is harmless.
- Register: if `awaitPendingSync()` times out (10 s), transaction 1 is already committed and the
  report is not. The user sees an error and can retry. The retry finds the month and skips step 3.
- D5 changes error behavior: errors that were only logged now fail the request, and checked
  `BusinessException` now rolls back. This is a visible failure instead of silent data loss.
- Hibernate may send `FOR NO KEY UPDATE` instead of `FOR UPDATE` for `PESSIMISTIC_WRITE` on
  PostgreSQL. Both block each other, so the result is the same.
- A flow that writes balances and skips the lock rule breaks the guarantee. The rule is in
  `.claude/CLAUDE.md` ("Horizontal scaling") and in D3, and the new lock-order tests check the main
  flows. Product-only edits are a non-goal.
- D8 makes the test service and the test lock static and shared across test classes. A stuck task
  affects the next class. `resetState()` waits first, and the 10 s timeout turns a stuck task into
  a clear error.
- `ProcessMovementServiceDatabaseLockTest` needs Docker. Without Docker, Dev Services cannot start,
  the `finance` datasource has no URL, and Quarkus fails to boot. That is why the test is opt-in
  (tag `database`).
- It is the first `@QuarkusTest` in `jbh-finance`. Setup risks, each with its fallback:
  - JPMS: Quarkus tests do not run on the module path, so the infra surefire uses
    `useModulePath=false`. Tests then run on the classpath.
  - JUnit versions: `jbh-finance/pom.xml` pins `junit-jupiter` to `${junit.version}` (5.10.2), and
    `quarkus-junit5` from the Quarkus BOM brings a newer JUnit. If the enforcer
    (`dependencyConvergence`) or the JUnit launcher fails, align the versions in the infra pom only.
  - Empty URL override: if Dev Services still see a URL and do not start, replace the URL override
    with a `QuarkusTestResourceLifecycleManager` that starts one Testcontainers PostgreSQL and returns
    the URL (the Testcontainers version comes from the Quarkus BOM).
  - Default datasource: if the boot fails with "datasource '<default>' is not configured", remove
    the `quarkus.datasource.devservices.enabled=false` override. A second, unused container is harmless.
- A race test does not fail on every run when the lock is missing. Five repetitions make a miss
  very unlikely, not impossible. Five green repetitions are strong evidence, not a proof.
