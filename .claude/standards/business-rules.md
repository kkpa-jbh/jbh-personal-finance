# Business Rules Distribution by Module

This document defines **where business rules belong** in the hexagonal architecture. Use this as the authoritative guide when deciding where to place a validation, constraint, or business condition — and when reviewing a module for correctness.

---

## Decision Rule

> **"Would a domain expert recognize this rule as part of the business, with no knowledge of the system?"**

| Answer | Module |
|--------|--------|
| Yes — purely conceptual business rule | **Domain** |
| Yes — but requires loading multiple aggregates or coordinating steps | **Application** |
| No — purely technical constraint | **Infrastructure** |

---

## Domain Module

**Owns rules intrinsic to the business itself** — a domain expert should recognize these without knowing anything about software.

### What belongs here

| Rule Type | Description | Example |
|-----------|-------------|---------|
| **Entity invariants** | Rules protecting the internal consistency of an aggregate | A transaction amount must be positive; an account cannot exceed its allowed overdraft |
| **Value object constraints** | Validation embedded directly in value object construction | Valid IBAN format; non-null category name |
| **Domain service rules** | Business logic spanning multiple entities but purely conceptual | Calculating net profit across transactions |

### Enforcement mechanism

- Constructors, static factory methods, and guard clauses
- Throw **domain exceptions** on violation
- **Never allow an entity or value object to exist in an invalid state**

### Examples

```java
// Value Object — constraint in constructor
public final class TransactionAmount {
    private final BigDecimal value;

    public TransactionAmount(final BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("Amount must be positive");
        }
        this.value = value;
    }
}

// Entity — invariant via factory method
public static Movement create(final TransactionAmount amount, final Category category) {
    Objects.requireNonNull(category, "Category is required");
    return new Movement(amount, category);
}
```

---

## Application Module

**Owns rules about coordinating the system to fulfill a use case**, not about the business concept itself.

### What belongs here

| Rule Type | Description | Example |
|-----------|-------------|---------|
| **Orchestration rules** | The sequence of steps to complete a use case | validate input → load aggregate → apply domain operation → persist → notify |
| **Cross-aggregate rules** | Conditions requiring state from multiple aggregates via ports | Verifying an account exists before associating a transaction |
| **Authorization rules** | Whether the current actor is permitted to execute the use case | Only the account owner may delete a movement |
| **Input validation at boundaries** | Structural/presence validation before reaching the domain | Required fields, data types — distinct from business validation, which belongs in the domain |

### Enforcement mechanism

- Use case classes calling domain-language port interfaces
- Catch domain exceptions and translate them into application-level responses
- **Never embed business rules that a domain expert would own**

### Examples

```java
// Input boundary validation (structural — not business)
public MovementDTO execute(final CreateMovementCommand command) {
    Objects.requireNonNull(command, "Command is required");
    Objects.requireNonNull(command.getAccountId(), "AccountId is required");

    // Cross-aggregate rule: account must exist
    final Account account = accountRepository.findById(command.getAccountId())
        .orElseThrow(() -> new AccountNotFoundException(command.getAccountId()));

    // Delegate actual business rule enforcement to domain
    final Movement movement = Movement.create(command.getAmount(), command.getCategory());

    movementRepository.save(movement);
    return MovementDTO.from(movement);
}
```

---

## Infrastructure Module

**Owns rules that are purely technical constraints with no business meaning.**

### What belongs here

| Rule Type | Description | Example |
|-----------|-------------|---------|
| **Persistence constraints** | Uniqueness, indexing, referential integrity at the DB level | `@Column(unique = true)`, FK constraints — mirror domain rules, never replace them |
| **Retry and fault-tolerance rules** | How many times to retry a failed external call, circuit breaker thresholds | Retry 3 times with exponential backoff |
| **Serialization rules** | Mapping domain objects to/from persistence or wire formats | JPA entity mapping, JSON serialization |

### Enforcement mechanism

- JPA constraints, adapter-level exception handling, and mapping logic
- **Never leak these concerns into domain or application layers**

### Examples

```java
// Persistence constraint — mirrors domain rule, does not replace it
@Entity
@Table(name = "movements", schema = "finance")
public class MovementEntity {

    @Column(name = "amount", nullable = false)
    private BigDecimal amount; // domain already enforces positivity; DB adds safety net
}
```

---

## Common Mistakes to Avoid

| Mistake | Where it was placed | Where it should be |
|---------|--------------------|--------------------|
| Null check on a value object field | Application use case | Domain — value object constructor |
| "Account must exist" check in a REST adapter | Infrastructure | Application use case |
| Business format validation (e.g., IBAN) in JPA entity | Infrastructure | Domain — value object |
| Authorization logic in a domain service | Domain | Application use case |
| Retry logic in an application use case | Application | Infrastructure adapter |

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────┐
│              WHERE DOES THIS RULE BELONG?                   │
├─────────────────────────────────────────────────────────────┤
│ Is it a business invariant a domain expert would know?      │
│   YES → Domain (entity/VO constructor or domain service)    │
│                                                             │
│ Does it require loading aggregates or coordinating steps?   │
│   YES → Application (use case orchestration)                │
│                                                             │
│ Is it purely a technical concern (DB, retry, serialization)?│
│   YES → Infrastructure (adapter or persistence layer)       │
└─────────────────────────────────────────────────────────────┘
```
