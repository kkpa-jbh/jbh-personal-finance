## ADDED Requirements

### Requirement: Balance writes for one product are serialized across instances

A flow that changes a product's balances MUST run in a database transaction and MUST lock the
product row as the first statement of that transaction. These flows are: process a movement,
register an official monthly report, upload movements, and the background adjust step. The protection MUST hold when the
app runs as 2 or more instances. It MUST NOT depend on in-JVM locks, caches or executors.

#### Scenario: Two instances add a movement to the same month

- **WHEN** instance A syncs a deposit of 5.00 and instance B syncs a withdrawal of -3.00 for the same
  product and month at the same time
- **THEN** one of them waits for the other's transaction to end, and the stored month has total
  debits 5.00, total credits 3.00, movement balance 2.00 and 2 movements

#### Scenario: Background adjust runs while another movement is synced

- **WHEN** the adjust step for a month runs and a new movement for the same product is synced
- **THEN** one of them runs fully before the other starts, and the later one reads the data the
  earlier one saved

#### Scenario: The lock is taken first

- **WHEN** a movement is added to a product
- **THEN** the product row is locked before the month or the product is read

#### Scenario: Lock outside a transaction

- **WHEN** the product lock is requested with no open transaction
- **THEN** the request fails at once instead of taking a lock that ends right away

### Requirement: A failed flow rolls back completely

If any step of a balance flow fails, including a checked `BusinessException`, the system MUST roll back
the whole transaction: the movement, the product and the monthly balance. The error MUST reach the
caller. It MUST NOT be only logged.

#### Scenario: Save of the month fails

- **WHEN** the monthly balance save throws during the movement flow
- **THEN** the same exception reaches the caller, the movement is not stored, the product balance
  is unchanged, and no adjust task is submitted

#### Scenario: Business validation fails

- **WHEN** the movement flow throws a `BusinessException` after it locked the product
- **THEN** the same `BusinessException` reaches the caller and nothing is committed

### Requirement: Monthly balance is saved before the sync call returns

When a new or reversed movement is synced, or `persistBalancesAsync` is called, the system MUST save
the updated monthly balance before the call returns, inside the caller's transaction. The save MUST
NOT depend on a background task.

#### Scenario: Two movements in the same month both stay

- **WHEN** a deposit of 5.00 and then a withdrawal of -3.00 are synced for the same product and
  the same month, one call right after the other
- **THEN** the stored monthly balance has total debits 5.00, total credits 3.00, movement
  balance 2.00 and 2 movements

#### Scenario: The save does not need the background executor

- **WHEN** a movement is synced and the background executor never runs its tasks
- **THEN** the stored monthly balance already holds the movement

#### Scenario: A reversed movement is removed from the stored month

- **WHEN** a deposit of 10.00 and a withdrawal of -4.00 are synced, then the withdrawal is reversed
- **THEN** the stored monthly balance has total credits 0.00, movement balance 10.00 and 1 movement

### Requirement: Adjust step runs in the background, in its own locked transaction

The adjust step (opening balance, monthly profit, next months, fully-withdrawn update) MUST run in
a background task. Each task MUST open its own transaction and lock the product first. Background
tasks of one executor instance MUST run one at a time, in the order they were submitted.

#### Scenario: Tasks run in submit order

- **WHEN** three tasks are submitted one after the other to the same executor instance
- **THEN** they start and finish in submit order

#### Scenario: A failed task does not stop the next task

- **WHEN** a task throws and another task is submitted after it
- **THEN** the first future completes exceptionally and the second task still runs

#### Scenario: Adjust locks the product before it reads

- **WHEN** an adjust task runs
- **THEN** it opens a transaction and locks the product before it reads the months

### Requirement: Callers can wait for pending background work

The executor MUST let a caller wait until every task submitted before the call on the same instance
has finished, with a timeout. The wait MUST end as soon as that work ends. It MUST NOT be a fixed sleep.
It MUST NOT be used inside a transaction.

#### Scenario: Wait ends after the earlier task

- **WHEN** a task that takes 200 ms is submitted and a caller then waits with a 5 s timeout
- **THEN** the wait returns after the task has finished, and before the timeout

#### Scenario: Nothing pending

- **WHEN** no task is pending and a caller waits
- **THEN** the wait returns at once

#### Scenario: Timeout

- **WHEN** a pending task does not finish within the timeout
- **THEN** the wait throws `IllegalStateException`

#### Scenario: Wait from the worker thread

- **WHEN** a background task itself calls the wait
- **THEN** the wait throws `IllegalStateException` at once instead of blocking forever

#### Scenario: An earlier task failed

- **WHEN** an earlier task threw an exception
- **THEN** the wait still returns normally

### Requirement: Register uses two locked transactions and waits between them

Registering an official monthly report MUST lock the product in each of its transactions. When the
month does not exist yet, it MUST add the initial balance movement in a first transaction, commit,
wait for its own pending sync outside any transaction, and then read the month and write the report
in a second transaction. It MUST NOT use a fixed sleep.

#### Scenario: Month already exists

- **WHEN** an official report is registered for a month that exists
- **THEN** the product is locked before the month is read, and there is no wait for pending sync

#### Scenario: First report of a product

- **WHEN** an official report is registered and the month does not exist yet
- **THEN** the initial balance movement is added and committed, pending sync is awaited outside any
  transaction, and then the product is locked again and the month is read, with no fixed 2 s delay

### Requirement: Upload runs in one locked transaction

Uploading movements from a file MUST lock the product, sync the product balance, save the movements
and the product, and save the monthly balances in one transaction. Only the adjust step runs later.

#### Scenario: Upload locks first

- **WHEN** valid movements are uploaded for a product
- **THEN** the product is locked before it is read, and all saves happen in the same transaction

### Requirement: Bulk persist saves first and returns the adjusted balances

`persistBalancesAsync` MUST save the given balances before it returns, then adjust them in the
background, and MUST return the adjusted balances in its future.

#### Scenario: Persist and adjust for one month

- **WHEN** `persistBalancesAsync` is called with one monthly balance and a next month exists
- **THEN** the balance is stored when the call returns, the future returns 2 balances, and the next
  month opening balance equals the first month closing balance
