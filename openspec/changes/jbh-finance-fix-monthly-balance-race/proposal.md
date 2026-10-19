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

## What Changes

- `ProcessMonthlyBalanceServiceImpl.syncForNewMovement` and `syncForReversedMovement` save the
  monthly balance **synchronously**, on the caller thread. Only the adjust step (opening
  balance, profit, next months, fully-withdrawn update) stays in the background.
- A private lock in `ProcessMonthlyBalanceServiceImpl` guards two blocks: the caller
  read-assign-save and the background adjust read-recalculate-save. They can no longer overwrite each other.
- `AsyncTaskExecutorImpl` uses **one shared single-thread executor per instance**. Background
  tasks run in submit order. The executor no longer shuts down after each task.
- `persistBalancesAsync` keeps its public contract (save, then adjust, in the background).
  `MonthlyBalanceSyncForUploadedMovements` does not change.
- New regression tests. Existing tests keep their expected values.
- No database change. No schema, no migration.

## Capabilities

### New Capabilities

- `monthly-balance-sync`: how a new or reversed movement is saved into its monthly balance, and
  how the follow-up adjust step runs in the background.

### Modified Capabilities

None. No spec exists in `openspec/specs/` yet.

## Impact

- Code: `jbh-finance/jbh-finance-application`
  - `ProcessMonthlyBalanceServiceImpl.java`
  - `AsyncTaskExecutorImpl.java`
- Tests: `jbh-finance/jbh-finance-application` (new regression test class, one new test method in
  `AsyncTaskExecutorImplTest`).
- Behavior: a failure while saving the monthly balance now reaches the caller. Before, it was only
  logged in the background thread.
- Tests that should pass after the fix: `RegisterMonthlyReportedProfitTest` (both failing
  methods) and `DeleteMovementUseCaseTest.deleteExpense30WithSnapshot`.
- Other modules: none. `jbh-finance-infra` still uses `new AsyncTaskExecutorImpl()`; no change.
