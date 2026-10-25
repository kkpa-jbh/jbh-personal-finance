# monthly-balance-sync — maintenance guide

> The map for changing this concept without re-scanning the codebase. Paths + symbols only;
> for current signatures/callers/callees, ask CodeGraph. Pin to file paths, never line numbers.
> All KB links are relative to `kkpa/context/`.

## Glossary

- **Known as:** `monthly balance sync`, `monthly balance`, `product row lock`
- **Internal name:** `monthly-balance-sync` (OpenSpec capability)
- **Type:** workflow — no single trigger. It runs inside four flows: process a movement,
  register an official monthly report, upload movements, and the background adjust step.

## Component map

Not curated yet. The spec has no file paths. Run `/kkpa-context-curate monthly balance sync`
to add the file map.

## How maintenance works

1. **Lock first.** Every flow that changes a product's balances opens one database transaction.
   Its first statement locks the product row. A second instance that touches the same product
   waits until the first transaction ends.
2. **Save the month now.** When a movement is synced (new or reversed), the updated monthly
   balance is saved inside the caller's transaction, before the call returns. It never waits for
   a background task.
3. **Adjust later.** The adjust step (opening balance, monthly profit, next months, fully-withdrawn
   update) runs as a background task. Each task opens its own transaction and locks the product
   first. Tasks of one executor instance run one at a time, in submit order. A failed task does
   not stop the next one.
4. **Register a report.** When the month exists: lock, read the month, write the report, in one
   transaction. When the month does not exist yet: transaction 1 adds the initial balance
   movement and commits; then the caller waits for its own pending sync, outside any
   transaction; then transaction 2 locks again, reads the month and writes the report.
5. **Upload movements.** One transaction: lock the product, sync the product balance, save the
   movements, the product and the monthly balances. Only the adjust step runs later.
6. **Bulk persist.** `persistBalancesAsync` saves the given balances before it returns, then
   adjusts them in the background. Its future returns the adjusted balances.
7. **On failure** any step rolls back the whole transaction (movement, product, monthly balance),
   and the error reaches the caller.

## Conventions & gotchas

Non-obvious rules that make a change correct. Each one cites its source.

- **Lock the product as the first statement of every balance-writing transaction.** Do not read
  the month or the product before the lock. This is what keeps 2 or more instances correct.
  _Source: spec monthly-balance-sync — Requirement: Balance writes for one product are serialized across instances._
- **Never protect balances with in-JVM state.** No in-JVM locks, caches or executors for
  correctness. The single-thread executor only gives order on one instance.
  _Source: spec monthly-balance-sync — Requirement: Balance writes for one product are serialized across instances._
- **The lock needs an open transaction.** Asking for it with no transaction fails at once. It does
  not take a lock that ends right away.
  _Source: spec monthly-balance-sync — Requirement: Balance writes for one product are serialized across instances._
- **Roll back on every error, a checked `BusinessException` included.** Rethrow to the caller;
  never only log it. On failure no adjust task is submitted.
  _Source: spec monthly-balance-sync — Requirement: A failed flow rolls back completely._
- **The monthly balance save must not depend on the background executor.** It happens in the
  caller's transaction, before the sync call returns.
  _Source: spec monthly-balance-sync — Requirement: Monthly balance is saved before the sync call returns._
- **Wait for background work with the executor wait, never a fixed sleep.** The wait has a timeout
  (`IllegalStateException` when it expires), returns at once when nothing is pending, and still
  returns when an earlier task failed.
  _Source: spec monthly-balance-sync — Requirement: Callers can wait for pending background work._
- **Never wait inside a transaction, and never from the worker thread.** A wait from a background
  task throws `IllegalStateException` at once instead of blocking forever.
  _Source: spec monthly-balance-sync — Requirement: Callers can wait for pending background work._
- **Register on a new month uses two locked transactions.** Commit the initial balance movement,
  wait outside any transaction, then lock again. No fixed 2 s delay.
  _Source: spec monthly-balance-sync — Requirement: Register uses two locked transactions and waits between them._
- **The wait only sees one instance.** It waits for tasks submitted on the same executor instance.
  All callers must share one executor per instance, or the wait returns early.
  _Source: spec monthly-balance-sync — Requirement: Callers can wait for pending background work._

## Related KB

- Source spec: `openspec/specs/monthly-balance-sync/spec.md` (repo root)
