# CATEGORIES

- A category to classify an income as CDT INCOME
- The default category is UNKNOWN.

# PRODUCTS

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

## USE CASES

### 2. ADD TRANSFER ACCOUNTS

- The user can add a transfer between two existing accounts.
- The user can create a transfer to pay a credit-card bill.

  ### CDT

    - To reintegrate the CDT amount to an existing saving account.

  ### CREDIT CARD

    - The deposits are always done via `transfer` movements (From saving account to credit card account).

# MOVEMENTS

## USE CASES

## 1. ADD MOVEMENT USE CASE

The user is able to add movements to any product it does not matter if it's in the past.
That should produce a cascade of synchronizations on the products balances along with the monthly balances.

### CDT

- To register the CDT payment to a third party.
- Once the payment is registered, the CDT account should be closed/inactive/finished.
- ALL CDT `Must` have a RETEFUENTE movement.

## 2. FIND MOVEMENTS USE CASE

### 2.1 FIND MOVEMENTS BY PRODUCT

- The product should be associated with the user.
- The use case returns the latest movements done in the last 3 months.

# MONTHLY BALANCES

- They dont accumulate data from previous months.
- the closingBalance for each monthly balance represents only the net of movements within that month (starting from zero), not an accumulated running balance.

# REMINDERS

## ACCOUNTS

- to notify that the CDT is about to expire.
- to notify that the Credit Card payment must be recorded.