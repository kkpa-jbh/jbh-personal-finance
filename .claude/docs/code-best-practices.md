# CRITICAL RULES

## Naming Conventions

This document defines **strict naming conventions and responsibilities** for backend classes to ensure:

- Clear separation of concerns
- Stable API contracts
- Maintainable and scalable architecture

These rules must be followed when generating or reviewing backend code.

---

### 1. Persistence Layer (Database / ORM)

#### Purpose

     - Represents database tables
     - Used only for persistence
     - Must never be exposed outside the persistence layer

#### Naming Convention

   ```
   <EntityName>Entity
   ```

#### Examples

- MonthlyBalanceEntity
    - UserEntity
    - TransactionEntity

#### Rules

- Used only by repositories
    - Contains ORM annotations
    - Never returned by API controllers

---

### 2. Internal Data Transfer Objects (Application Submodules)

#### Purpose

- Used for internal data transfer between layers
- Represents domain-level data
- May contain business-related fields

#### Naming Convention

   ```
   <EntityName>DTO
   ```

#### Examples

- MonthlyBalanceDTO
- UserDTO

#### Rules

- Internal use only
- Must not be serialized directly to API responses
- Free to change as business logic evolves

---

### 3. API Request Objects (Inbound API)

#### Purpose

- Represents data received from API clients
- Defines the input API contract
- Only present in the jbh-xxxx-infra modules.

#### Naming Convention

```
<EntityName>Request
```

#### Examples

- MonthlyBalanceRequest
- CreateUserRequest

#### Rules

- Used only in API controllers
- Validated at the API boundary
- Never reused as domain or persistence models

---

### 4. API Response Objects (Outbound API)

#### Purpose

- Represents data returned to API clients
- Defines the public API contract
- Optimized for client consumption
- Only present in the jbh-xxxx-infra modules.

#### Naming Convention

```
<EntityName>Response

// or when neede it
<EntityName>ApiResponse
```

#### Examples

- MonthlyBalanceResponse
- UserResponse

#### Rules

- Must NEVER be named `DTO`
- Must NEVER expose Entity classes
- Can differ in structure from internal DTOs
- Must be stable and versionable

---

### 5. Mandatory Mapping Flow

All backend data transformations must follow this flow:

```
<EntityName>Entity
↓
<EntityName>DTO
↓
<EntityName>Response
```

Skipping layers is not allowed unless explicitly instructed.

---

### 6. Forbidden Patterns ❌

The following are not allowed:

- Returning `*DTO` from API controllers
- Returning `*Entity` from API controllers
- Naming API responses with `DTO`
- Mixing persistence and API concerns in the same class

---

### 7. Enforcement Rules

- Controllers use `*Request` and `*Response`
- Services use `*DTO`
- Repositories use `*Entity`
- When unclear, assume full separation of layers

---

### 8. Correct Example

```
MonthlyBalanceEntity
MonthlyBalanceDTO
MonthlyBalanceRequest
MonthlyBalanceResponse
```

---

### Key Principle

**DTOs are internal.  
Responses are contracts.  
Entities are persistence-only.**

## Code Conventions

1. Boolean Parameter Anti-Pattern (Do not used it on any method). It's only allowed to use it in the constructors.
2. A method/function should not have more than 3 arguments. Introduce value objects to improve it. (Domain Value Objects)
   Example: This is a wrong/antipattern function. It has 4 arguments

```java
public List<MonthlyBalanceDTO> findBalancesByProductIdsAndPeriods(
    final List<ProductId> productIds, final YearMonth startPeriod, final YearMonth endPeriod, final boolean endPeriodExclusive);
```

And can be refactored like this:

```java
// Introducing a domain value object PeriodRange

public List<MonthlyBalanceDTO> findBalancesByProductIdsAndPeriods(
    final List<ProductId> productIds,
    final PeriodRange periodRange);

// Callers
findBalancesByProductIdsAndPeriods(productIds, PeriodRange.inclusive(start, end));
```

3. The methods should be self-documenting.
4. Value Object characteristics: Provide name constructors.

## Best Practices Enforcement

- SOLID principles application
- Proper exception handling and logging
- Performance optimization recommendations
- Clean code practices
- Every module should have a highly cohesion
- Every single code block generated should use final variables and final arguments.
- Modules demonstrate proper separation of concerns
- Code follows established patterns and conventions
- Solutions are cost-effective and deployment-ready

### Architecture Recommendations

- Explain reasoning behind architectural decisions
- Provide alternative approaches when applicable
- Include trade-off analysis for different solutions
- Reference SOLID principles and clean architecture concepts
- The system is divided into self-contained modules with clear responsibilities.

### Design Patterns/Principles

Always try to apply the following principles:

- SRP (Single Responsability Principle)
- SOLID
- The repositories should only be included into service classes.
- The Input port classes should have dependency of services rather than repositories.

### Documentation

- Include clear explanations for complex implementations
- Provide setup and configuration instructions
- Document API contracts and data models
- Explain integration patterns between modules