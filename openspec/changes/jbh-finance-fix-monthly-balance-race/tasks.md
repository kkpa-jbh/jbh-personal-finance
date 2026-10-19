## 1. Shared ordered executor (independent)

- [ ] 1.1 Change `AsyncTaskExecutorImpl` (`jbh-finance-application/src/main/java/com/jbh/finance/application/async/`): one `ThreadPoolExecutor` per instance (core 1, max 1, unbounded queue, daemon threads, `allowCoreThreadTimeOut(true)`); remove the per-task executor and the `shutdown()/close()` in `finally`. Keep logging, DLQ and metadata behavior. Depends on: none.
- [ ] 1.2 Append `shouldRunTasksInSubmitOrder` to `AsyncTaskExecutorImplTest` (contract in design.md, Test contract). Do not edit existing methods. Depends on: none (written against the contract).

## 2. Synchronous save in the monthly balance sync (independent of group 1)

- [ ] 2.1 Change `ProcessMonthlyBalanceServiceImpl` (D1): in `syncForNewMovement` and `syncForReversedMovement`, call `saveMultiBalances` on the caller thread, then submit only the adjust task through one private method (adjust + fully-withdrawn update). Keep `persistBalancesAsync` contract and body. Depends on: none.
- [ ] 2.2 In the same class add one `private final Object` lock (D3). Use it around the caller read-assign-save block, the adjust step, and the save + adjust part of the `persistBalancesAsync` task. Keep the fully-withdrawn update outside the lock. Depends on: 2.1 (same file).
- [ ] 2.3 Add `ProcessMonthlyBalanceServiceImplTest` with the 4 tests in design.md (Test contract). Depends on: none (written against the contract; compiles against the unchanged public interface).

## 3. Verification (needs groups 1 and 2)

- [ ] 3.1 Confirm `MonthlyBalanceSyncForUploadedMovements` and the tests that call `persistBalancesAsync` need no change (design.md says no). Depends on: 2.1, 2.2.
- [ ] 3.2 Leader or user runs the suites below. Expected: `RegisterMonthlyReportedProfitTest` (all methods), `DeleteMovementUseCaseTest.deleteExpense30WithSnapshot`, `AsyncTaskExecutorImplTest`, `ProcessMonthlyBalanceServiceImplTest`, `RegisterMovementExecutionTest`, `UploadMultiMovementsExecutionTest` all pass. Depends on: 1.1, 1.2, 2.1, 2.2, 2.3.

Suite commands (not run by the worker):

```
mvn -pl jbh-finance/jbh-finance-application test -Dtest='RegisterMonthlyReportedProfitTest,DeleteMovementUseCaseTest,AsyncTaskExecutorImplTest,ProcessMonthlyBalanceServiceImplTest,RegisterMovementExecutionTest,UploadMultiMovementsExecutionTest'
mvn -pl jbh-finance/jbh-finance-application test
```
