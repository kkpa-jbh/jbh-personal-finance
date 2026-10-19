## ADDED Requirements

### Requirement: Monthly balance is saved before the sync call returns

When a new or reversed movement is synced, the system MUST save the updated monthly balance
before `syncForNewMovement` or `syncForReversedMovement` returns. The save MUST NOT depend on a
background task.

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

### Requirement: Save failures reach the caller

If saving the monthly balance fails during a new or reversed movement sync, the system MUST let the
error reach the caller. It MUST NOT hide it in a background task.

#### Scenario: Save fails

- **WHEN** the monthly balance save throws during `syncForNewMovement`
- **THEN** the same exception reaches the caller and no adjust task is submitted

### Requirement: Adjust step runs in the background, in submit order

The adjust step (opening balance, monthly profit, next months, fully-withdrawn update) MUST run in
a background task. Background tasks of one executor instance MUST run one at a time, in the order
they were submitted.

#### Scenario: Tasks run in submit order

- **WHEN** three tasks are submitted one after the other to the same executor instance
- **THEN** they start and finish in submit order

#### Scenario: A failed task does not stop the next task

- **WHEN** a task throws and another task is submitted after it
- **THEN** the first future completes exceptionally and the second task still runs

### Requirement: Caller sync and background adjust do not overwrite each other

The system MUST NOT run the caller read-assign-save block and the background adjust block of the same service instance at the same time.

#### Scenario: Adjust of movement 1 runs while movement 2 is synced

- **WHEN** the adjust of movement 1 is running and movement 2 is synced for the same month
- **THEN** movement 2 reads the month only after the adjust has saved, and keeps both movements

### Requirement: Bulk persist keeps its contract

`persistBalancesAsync` MUST still save the given balances and then adjust them in the background, and
MUST return the adjusted balances in its future.

#### Scenario: Persist and adjust for one month

- **WHEN** `persistBalancesAsync` is called with one saved monthly balance and a next month exists
- **THEN** the future returns 2 balances and the next month opening balance equals the first month
  closing balance
