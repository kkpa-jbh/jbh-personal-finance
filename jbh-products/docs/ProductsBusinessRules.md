# CATEGORIES

- A category to classify an income as CDT INCOME
- The default category is UNKNOWN.

# ACCOUNTS

## 1. CREDIT CARD

> metadata: payment_due_date, credit_limit,

- When adding a movement to the credit card account, it is always a `withdrawal` movement.
- The deposits are always done via `transfer` movements.
- I cannot assume that the user is going to register all the payments done with the credit card,
  so that's why I will not go for any interest calculations.

## 2. CDT

> metadata: An Array of interest_rate, terms (days), payment_due_date

- It's possible to create a CDT account for old dates (before the current date); That means, that any saving account
  created the first movement or the initial balance of the CDT.
- It's a fixed-term saving account.
- You deposit money for a predetermined period and receive a guaranteed interest rate
- Similar to a time deposit or term deposit in other countries
- Payment due date refers to the date when investment is due and the user receives the amount plus the interest.
- If a CDT is renegotiated, we add a new element to the array metadata
- The CDT is only closed when the user withdraws the money in the fixed period (Receiving it in a saving account to transfering it to a third party)
- A reminder will be sent to the user to notify that the CDT is due.

# USE CASES

## 1. ADD MOVEMENT USE CASE

### CDT

- To register the CDT payment to a third party.
- Once the payment is registered, the CDT account should be closed/inactive/finished.
- ALL CDT `Must` have a RETEFUENTE movement.

## 2. ADD TRANSFER ACCOUNTS

- The user can add a transfer between two existing accounts.
- The user can create a transfer to pay a credit-card bill.

  ### CDT

    - To reintegrate the CDT amount to an existing saving account.

  ### CREDIT CARD

    - The deposits are always done via `transfer` movements (From saving account to credit card account).

# REMINDERS

## ACCOUNTS

- to notify that the CDT is about to expire.
- to notify that the Credit Card payment must be recorded.