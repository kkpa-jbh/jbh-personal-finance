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

Production uses the same code: `ProductUseCasesCDIConfig` builds `new AsyncTaskExecutorImpl()` in
the `processMonthlyBalanceService()` producer, which is `@ApplicationScoped`. So there is one executor
instance in production.

Callers of `persistBalancesAsync` (checked):

- `ProcessMonthlyBalanceServiceImpl.syncForNewMovement` and `syncForReversedMovement`: change.
- `MonthlyBalanceSyncForUploadedMovements.persistBalancesAsync` and
  `syncForUploadedMovementsAsync`: **do not change**. They call the public method, which keeps its contract.
  They already rely on "save, then adjust, in the background". They gain the submit order for free.
- Tests `RegisterMovementExecutionTest` and `UploadMultiMovementsExecutionTest` call it and read
  `.get()`. The contract is kept, so they stay as they are.

## Goals / Non-Goals

**Goals**

- Two movements in the same month, one after the other, both stay.
- Background tasks run in submit order.
- Fix `RegisterMonthlyReportedProfitTest` (`registerReportWithRetefuente20252` and
  `shouldAddDebitsToReportedMonthlyBalance`) and `DeleteMovementUseCaseTest.deleteExpense30WithSnapshot`
  without editing any assertion.

**Non-Goals**

- Concurrent HTTP requests for the same product. Two requests on two threads can still lose an
  update at database level. A fix needs row locks or versions. That is a different change.
- Making the upload flow (`syncForUploadedMovementsAsync`) read-after-save safe. Only its order improves.
- Database change. None. No table, column, index, constraint or migration.
- Replacing `YearMonth.now()` in the adjust step (existing FIXME).
- Renaming `AsyncTaskExecutorImplTest.shouldExecuteMultipleTasksConcurrently` (see D2).
- A DLQ or retry for failed adjust tasks (existing FIXME).

## Decisions

### D1: Save synchronously in the caller, adjust in the background

`syncForNewMovement` and `syncForReversedMovement` do: read, assign (or reverse), then
`saveMultiBalances` on the caller thread. Then they submit **only** the adjust step to the executor.

The adjust task body is: `adjustCurrentAndNextMonthlyBalancesAsync(productId, period, period)`, then
the fully-withdrawn update (`isFullyWithdrawn` / `updateWhenFullyWithdrawn`). This is the old task
body without the save line. It moves to one private method used by both sync methods.

`persistBalancesAsync` keeps its old body (save, adjust, fully-withdrawn, all in the background).

Why: the next movement must read a month that is already saved. A read that follows a finished
save cannot start from an empty month.

Rejected: wait for the future inside the sync (`persistBalancesAsync(...).join()`). It blocks the
request on the whole adjust step, which is slow for old periods, and it removes the async design.

Returned value: `syncedMonthlyBalanceDTO` is the same object as before (balance after the
movement, before the adjust). Callers see no change.

Error behavior changes on purpose: a save error now reaches the caller. Before, it was only logged
in the background thread. The adjust step is still fire and forget, as before.

### D2: One shared single-thread executor per `AsyncTaskExecutorImpl` instance

`AsyncTaskExecutorImpl` holds one `ThreadPoolExecutor` field: core 1, max 1, unbounded queue, a
daemon thread factory, and `allowCoreThreadTimeOut(true)` with a keep-alive. `submitTask` submits to
it. The `finally { shutdown(); close(); }` block is removed.

Why:

- Same executor, one thread: tasks run in submit order, one at a time.
- Daemon thread: it does not block JVM shutdown.
- Core thread timeout: tests create many `AsyncTaskExecutorImpl` instances. An idle thread ends by
  itself, so threads do not pile up. A new thread starts on demand. Order is kept because there is
  still at most one thread.
- One instance in production, so order is global, not per product (user decision).

Rejected: one executor per product (more threads, more code, not asked). A static shared executor
(hides state, couples separate service instances in tests).

`AsyncTaskExecutorImplTest.shouldExecuteMultipleTasksConcurrently` asserts only each result and
`isDone` with a 5 s timeout. Three tasks of 50 ms run serial in about 150 ms and give the same
results. It still passes. The name is now untrue, but the project rule is append-only, so it
stays. Other tests in that class (interrupt, null metadata, exception metadata) do not depend
on a thread per task. The interrupt test: the worker thread is interrupted by the task; the pool
clears the flag before the next task, so later tasks are not affected.

### D3: A private lock guards the two read-modify-write blocks

With D1 alone a new race remains. Adjust 1 (background) reads the month, then movement 2
(caller) reads and saves, then adjust 1 saves its older copy over movement 2. Movement 2 is lost.

`ProcessMonthlyBalanceServiceImpl` gets one `private final Object` lock. It guards:

- the caller block: read the month, assign or reverse, save (in D1);
- the background block: `adjustCurrentAndNextMonthlyBalancesAsync` (read next balances, recalculate,
  save), and the save + adjust part of the `persistBalancesAsync` task.

The fully-withdrawn update is outside the lock. Neither block waits for the future while holding the
lock, and the background block never waits for the caller block. No deadlock is possible.

Result: adjust 1 either finishes before movement 2 reads (movement 2 sees the adjusted month), or
starts after movement 2 saves (adjust 1 reads fresh data, because it reads at run time).

Rejected: a lock per product (more code; there is no declared performance profile; one lock per
service instance is simple and enough). A DB version column (database change, not wanted).
Scope of the lock is one JVM, which matches the non-goal on multiple requests at database level.

No new class, no new interface. This lock is a field of an existing class.

### D4: SOLID scope

SRP only. DIP already holds: the service uses the `AsyncTaskExecutor` interface. No new interface.
The regression test uses the existing interface with a small test-side implementation.

### D5: No database change

No schema, index or migration. Reads and writes use the same repository methods as before,
`findByAccountIdAndPeriod` and `saveMultiBalances`. Only the thread and the order change.
The save now runs on the caller thread. The repository adapters already work without an HTTP
request context (they ran on a plain background thread before), so they work on the caller thread too.

## Responsibilities

- `ProcessMonthlyBalanceServiceImpl` (changed): syncs the monthly balance with a new or reversed
  movement, saves it, then starts the background adjust step. Gets one more job: the lock that keeps
  caller sync and adjust from overwriting each other. This is part of the same job (safe sync of
  one monthly balance). Changes when: monthly balance sync rules change.
- `AsyncTaskExecutorImpl` (changed): runs background tasks in order on one shared thread. Changes
  when: the async execution model changes.
- `ProcessMonthlyBalanceServiceImplTest` (new test class): proves that two movements in the same month
  both stay. Changes when: the sync contract changes.
- `AsyncTaskExecutorImplTest` (one new method): proves submit order. Changes when: the execution
  model changes.
- New production classes: none.

## Test contract

Written before the implementation. Tests are offline unit tests in `jbh-finance-application`
(in-memory fakes). Existing expected values are **not** changed.

### New: `ProcessMonthlyBalanceServiceImplTest`

Package `com.jbh.finance.test.application.feature.monthlybalance.services`. Uses the in-memory
monthly balance fakes, `ProductLifecycleService` from `UseCaseFixtureBuilder`, and a fresh product
and a fresh `ProcessMonthlyBalanceServiceImpl` per test. Period: 2025-03 (in the past, so the adjust
step runs). Money values use `withJBHDecimals`.

| Test | Input | Expected |
|---|---|---|
| `shouldKeepBothMovementsWhenTwoMovementsAreSyncedInTheSameMonth` | real `AsyncTaskExecutorImpl`; `syncForNewMovement` deposit +5 on 2025-03-01, then at once withdrawal -3 on 2025-03-01; then wait for a no-op sentinel task submitted to the same executor | stored 2025-03: totalDebits 5.00, totalCredits 3.00, movementBalance 2.00, totalMovements 2 |
| `shouldSaveMonthlyBalanceBeforeReturningWhenExecutorNeverRuns` | executor stub that drops every task; one `syncForNewMovement` deposit +5 | stored 2025-03 already has totalDebits 5.00, totalMovements 1; the stub received exactly 1 task |
| `shouldRemoveReversedMovementFromStoredMonthWhenReversalIsSynced` | stub executor that drops tasks; sync deposit +10, sync withdrawal -4, then `syncForReversedMovement` of the withdrawal | stored 2025-03: totalDebits 10.00, totalCredits 0.00, movementBalance 10.00, totalMovements 1 |
| `shouldPropagateExceptionWhenSaveFails` | `MonthlyBalanceLifecycleService` mock whose `saveMultiBalances` throws `IllegalStateException`; stub executor | exception reaches the caller; stub received 0 tasks |

The stub is a lambda-style implementation of the existing `AsyncTaskExecutor` that counts calls and
returns a never-completed future. If `ProductLifecycleService` or the lifecycle service is easier
as a Mockito mock for the last test, use Mockito (the project allows mocks for interaction checks).

### New method in `AsyncTaskExecutorImplTest` (append only)

`shouldRunTasksInSubmitOrder`: submit 3 tasks on one instance. Task 0 sleeps 100 ms, task 1 sleeps
10 ms, task 2 sleeps 0 ms. Each appends its number to a synchronized list. After all three futures
complete, the list is `[0, 1, 2]`.

### Existing tests (expected values unchanged)

1. `RegisterMonthlyReportedProfitTest.registerReportWithRetefuente20252` (order 7). Registering
   2025-02 (closing 582, profit 5, retefuente 3) must create, in 2025-03, a dividend deposit +5 and a
   retefuente withdrawal -3. Expected 2025-03 after the 1 s wait: openingBalance 580.00,
   closingBalance 582.00, totalDebits 5.00, totalCredits 3.00, movementBalance 2.00,
   totalMovements 2, officialMonthlyReport false. Product: `movementBalance` = previous + profit
   (5) - retefuente (3), `netProfitBalance` 262.00, `currentBalance` = command closing balance. Why it passes
   now: both movements are saved before the next one is read (D1); adjust runs in order and sets
   opening 580 (D2, D3).
2. `RegisterMonthlyReportedProfitTest.shouldAddDebitsToReportedMonthlyBalance` (order 18). Expected
   product `currentBalance` and `movementBalance` equal `FIRST_BALANCE_ZERO`, i.e. 582 in the
   shared static `finalAccountBalance`. It failed with 575 only because test 7 stopped before
   `refreshActualAccountBalance()`. No code change for it. It passes when test 7 passes.
3. `DeleteMovementUseCaseTest.deleteExpense30WithSnapshot` (order 21). Expected product
   currentBalance 160.00, netProfitBalance 10.00, netGrowthRate 6.67, and current month
   `monthlyNetProfit` 10.00 (line 327; the old run gave 110). Closing balance equals 160.00.
   The movement is gone from the repository.

#### Check: can `deleteExpense30WithSnapshot` still read before the adjust ends?

`monthlyNetProfit` is set only by the adjust step (`recalculateBalances` in the adjust
loop). The test calls `deleteMovementUseCase.deleteMovement(...)` and then reads. The monthly
balances are read through `getCurrentMonthlyBalances()`, which calls `delayTests()` first. That is
a 500 ms wait. The product is read by `getCurrentProductInfo()` with no wait, but the product is
saved in the unit of work before the sync, so it needs no adjust.

The old value 110 came from out-of-order tasks: older tasks, running in parallel, wrote over newer
data. With D1 + D2 + D3 the tasks run in order and each reads fresh data. The in-memory adjust
takes a few ms, so the existing 500 ms wait is enough. **No test change is needed and no new wait.**
If the user still sees a read-before-finish failure, the next step is a test-side wait that is
more exact than 500 ms. Candidate: a test helper that submits a no-op sentinel to the executor of
the service under test and waits for it. That works only because tasks are in order (D2). It needs a
way to reach the executor, so it is not planned here. Assertions stay as they are.

## Risks / Trade-offs

- A global lock per service instance serializes balance syncs across products. The sections are short
  (in-memory work plus a save). No performance profile is declared. If a profile appears later, move
  to a lock per product.
- `UseCaseFixtureBuilder.buildProcessMonthlyBalanceSrv()` creates a new service and a new executor
  each call. Two services in one test have two executors and two locks, with no order between them.
  The existing tests that matter use one service chain per use case; if a test shows
  cross-instance order problems, share one executor in the builder. Not planned.
- The single worker thread means one slow adjust delays all later adjusts. This is the
  price of order, and was accepted in the interview.
- A save error now fails the movement request after the movement and product are already
  persisted in the unit of work. Before, the failure was silent. This is a visible failure instead of
  silent data loss. A full rollback is out of scope.
