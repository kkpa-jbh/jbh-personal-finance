# Feature-Based Package Organization

## Overview

The application module uses **feature-based organization** where code is grouped by business capability (vertical slices) rather than technical role (horizontal layers).

## Why Feature-Based Organization?

- **High Cohesion**: Everything related to a feature is together
- **Easy Navigation**: "I need product APIs? Go to `product/`"
- **Independent Changes**: Modify one feature without touching others
- **Team Ownership**: Teams can own entire vertical slices
- **Aligns with DDD**: Matches bounded contexts
- **Microservices-Ready**: Each folder could become a separate service

## Application Module Structure

```
com.jbh.xxx.application/
├── acid/                      # ACID transaction management (UnitOfWork)
├── async/                     # Async task execution framework
├── common/                    # Common utilities (logging, factories)
├── feature/                   # Business features parent package
│   ├── <feature-name>/        # Feature bounded context (e.g., product, movement)
│   │   ├── dto/               # Internal DTOs for this feature
│   │   ├── ports/
│   │   │   ├── input/         # Input ports (use case interfaces)
│   │   │   └── output/        # Output ports (repository interfaces)
│   │   ├── usecases/          # Use case implementations
│   │   ├── services/          # Domain services for this feature
│   │   ├── mappers/           # Mappers (Domain ↔ DTO)
│   │   ├── validation/        # Feature-specific validators
│   │   └── commands/          # Command objects for this feature
│   └── ...                    # Other features
└── shared/                    # Shared business concerns
    ├── exceptions/            # Application-level exceptions
    └── validation/            # Shared validators
```

## Package Organization Rules

### 1. Cross-Cutting Infrastructure (acid/, async/, common/)

**Purpose**: Technical services used across all features

**Examples**:
- `acid/`: UnitOfWork, TransactionManager
- `async/`: AsyncTaskExecutor, JobScheduler
- `common/`: LoggingContext, ObjectFactory

**Rules**:
- NEVER put business logic here
- Only framework-agnostic technical utilities
- Can be used by any feature

### 2. Feature Packages (feature/<feature-name>/)

**Purpose**: Business features as vertical slices (bounded contexts)

**Characteristics**:
- Each feature is self-contained
- Contains ALL business logic for that feature
- Clear boundaries with other features
- Microservices-ready (can be extracted easily)

**Examples**:
- `feature/product/`: Product management
- `feature/movement/`: Financial movements
- `feature/reporting/`: Reports and analytics

### 3. Shared Business (shared/)

**Purpose**: Business-level code used by multiple features

**Examples**:
- `shared/exceptions/`: BusinessException, ValidationException
- `shared/validation/`: CommandValidator, RuleEngine

**Rules**:
- NOT for infrastructure concerns
- Only domain/business-level shared code

## Feature Package Internal Structure

```
feature/<feature-name>/
├── dto/              # Data Transfer Objects for this feature only
├── ports/
│   ├── input/        # Use case interfaces (input ports)
│   └── output/       # Repository interfaces (output ports)
├── usecases/         # Use case implementations
├── services/         # Domain services specific to this feature
├── mappers/          # Domain ↔ DTO mappers for this feature
├── validation/       # Feature-specific validators
└── commands/         # Command objects for this feature
```

### Package Responsibilities

#### dto/
Internal DTOs representing business data for this feature.

**Example**: `ProductDTO`, `ProductSummaryDTO`

#### ports/input/
Use case interfaces defining business operations.

**Example**: `CreateProductInputPort`, `FindProductInputPort`

#### ports/output/
Repository interfaces defining data access needs.

**Example**: `ProductRepositoryPort`, `ProductQueryPort`

#### usecases/
Use case implementations containing business logic.

**Example**: `CreateProductUseCase`, `FindProductUseCase`

#### services/
Domain services performing complex business calculations.

**Example**: `BalanceCalculationService`, `InterestCalculationService`

#### mappers/
Converters between domain entities and DTOs.

**Example**: `ProductMapper`, `MovementMapper`

#### validation/
Feature-specific validation rules.

**Example**: `ProductValidator`, `TransferValidator`

#### commands/
Command objects representing business operations.

**Example**: `CreateProductCommand`, `UpdateBalanceCommand`

## Naming Conventions

- **Feature packages**: lowercase, singular (e.g., `product`, `movement`, `user`)
- **Use case interfaces**: `*InputPort` (e.g., `CreateProductInputPort`)
- **Use case implementations**: `*UseCase` (e.g., `CreateProductUseCase`)
- **DTOs**: `*DTO` (e.g., `ProductDTO`)
- **Commands**: `*Command` (e.g., `CreateProductCommand`)
- **Mappers**: `<Entity>Mapper` (e.g., `ProductMapper`)

## Module Exports (module-info.java)

### What to Export

Only export the public API of each feature:

```java
// Export feature public APIs
exports com.jbh.xxx.application.feature.<feature>.dto;
exports com.jbh.xxx.application.feature.<feature>.ports.input;
exports com.jbh.xxx.application.feature.<feature>.usecases;

// Export shared public APIs
exports com.jbh.xxx.application.shared.exceptions;
```

### What NOT to Export

Keep internal implementation details hidden:

```java
// Do NOT export these packages:
// - services (internal domain services)
// - validation (internal validators)
// - commands (internal command objects)
// - mappers (internal converters)
// - ports.output (internal repository interfaces)
```

### Complete Example

```java
module jbh.finance.application {
  // Dependencies
  requires jbh.finance.domain;
  requires org.slf4j;

  // Export product feature public API
  exports com.jbh.finance.application.feature.product.dto;
  exports com.jbh.finance.application.feature.product.ports.input;
  exports com.jbh.finance.application.feature.product.usecases;

  // Export movement feature public API
  exports com.jbh.finance.application.feature.movement.dto;
  exports com.jbh.finance.application.feature.movement.ports.input;
  exports com.jbh.finance.application.feature.movement.usecases;

  // Export shared APIs
  exports com.jbh.finance.application.shared.exceptions;

  // Do NOT export internal packages
  // com.jbh.finance.application.feature.product.services
  // com.jbh.finance.application.feature.product.validation
  // com.jbh.finance.application.feature.product.commands
  // com.jbh.finance.application.feature.product.mappers
  // com.jbh.finance.application.feature.product.ports.output
}
```

## Benefits

### 1. Team Autonomy
Teams can own entire features without stepping on each other's toes.

### 2. Clear Boundaries
Feature boundaries make it obvious where code belongs.

### 3. Reduced Coupling
Features communicate through well-defined ports, not direct dependencies.

### 4. Easier Testing
Test entire features in isolation without complex setup.

### 5. Migration Path
Extract features into microservices when needed without massive refactoring.
