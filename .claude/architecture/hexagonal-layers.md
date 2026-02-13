# Hexagonal Architecture - Module Responsibilities

## Overview

The hexagonal architecture separates the application into distinct layers with clear responsibilities and dependency rules. This document defines what belongs in each module and how they interact.

## Critical Rule: API Object Placement

**CRITICAL RULE:** Request and Response objects belong in the **infra** module, NOT the application module.

### Why This Matters

- **DTOs** contain business logic and can change with domain requirements
- **Response objects** define stable API contracts for external consumers
- **Infra layer** handles all external communication (HTTP, DB, messaging)
- **Application layer** remains pure business logic, unaware of HTTP/JSON concerns

## Application Module (`jbh-xxx-application`)

### Responsibilities

The application module contains:

1. **Business Logic**: Use cases, domain services, business workflows
2. **DTOs**: Data Transfer Objects for inter-layer communication
3. **Ports**: Interfaces defining inputs (use cases) and outputs (repositories)
4. **Commands**: Command objects representing business operations
5. **Validators**: Business rule validation logic
6. **Mappers**: Domain ↔ DTO conversion logic

### Package Structure

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

### What Belongs Here

**✅ YES - Application Module:**
- Business use cases (e.g., `CreateProductUseCase`)
- Use case interfaces (e.g., `CreateProductInputPort`)
- Repository interfaces (e.g., `ProductRepositoryPort`)
- DTOs for business data (e.g., `ProductDTO`)
- Command objects (e.g., `CreateProductCommand`)
- Domain services (e.g., `BalanceCalculationService`)
- Validators (e.g., `ProductValidator`)
- Mappers (e.g., `ProductMapper`)

**❌ NO - NOT Application Module:**
- REST Request/Response objects (belong in infra)
- JAX-RS annotations (`@Path`, `@GET`, etc.)
- JPA/Hibernate entities
- Database-specific code
- HTTP/JSON concerns
- Framework-specific code (Quarkus, Spring)

## Infrastructure Module (`jbh-xxx-infra`)

### Responsibilities

The infrastructure module contains:

1. **REST Adapters**: JAX-RS endpoints exposing the API
2. **Request/Response Objects**: API contracts for external consumers
3. **Database Adapters**: JPA repositories, entity mappings
4. **Persistence Entities**: Database table mappings
5. **External Service Adapters**: Integration with third-party APIs
6. **Configuration**: Quarkus configuration, CDI beans

### Package Structure

```
com.jbh.finance.infra/
├── adapters/
│   ├── in/
│   │   └── rest/
│   │       ├── <feature>/
│   │       │   ├── request/         # API request objects
│   │       │   ├── response/        # API response objects
│   │       │   └── *RestAdapter.java
│   │       └── common/
│   └── out/
│       └── persistence/
│           ├── entity/              # JPA entities
│           └── repository/          # JPA repositories
├── config/                          # Quarkus configuration
└── exception/                       # HTTP exception handlers
```

### Response Objects

**Purpose**: Define stable API contracts for external consumers

- **Package**: `*.infra.adapters.in.rest.<feature>.response`
- **Usage**: Controller method return types ONLY
- **Naming**: Must use `*Response` suffix, NEVER `*DTO`
- **Mapping**: Convert from DTOs using `fromDTO()` factory methods

**Example:**
```java
package com.jbh.finances.infra.adapters.in.rest.product.response;

import com.jbh.finance.application.feature.product.dto.ProductDTO;

public record ProductResponse(...) {
  public static ProductResponse fromDTO(final ProductDTO dto) {
    return new ProductResponse(...);
  }
}
```

### Request Objects

**Purpose**: Define API input contracts

- **Package**: `*.infra.adapters.in.rest.<feature>.request`
- **Usage**: Controller method parameters ONLY
- **Naming**: Must use `*Request` suffix
- **Validation**: Can include JAX-RS validation annotations

**Example:**
```java
package com.jbh.finances.infra.adapters.in.rest.product.request;

public record CreateProductRequest(...) {
  // API-level validation
}
```

### Entities

**Purpose**: Database persistence objects

- **Package**: `*.infra.adapters.out.persistence.entity`
- **Usage**: JPA/Hibernate ONLY
- **Exposure**: NEVER exposed outside persistence layer
- **Naming**: `*Entity` suffix

## Mapping Pattern

### Complete Flow

```java
// 1. REST Adapter (Infrastructure Layer)
package com.jbh.finances.infra.adapters.in.rest.product;

import com.jbh.finance.infra.adapters.in.rest.product.response.ProductResponse;
import com.jbh.finance.infra.adapters.in.rest.product.request.CreateProductRequest;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;

@Path("/products")
public class ProductRestAdapter {

  @Inject
  CreateProductInputPort createProduct;

  @GET
  public ProductResponse getProduct() {
    // Call use case, get DTO
    ProductDTO dto = createProduct.execute();

    // Convert DTO to Response for API
    return ProductResponse.fromDTO(dto);
  }
}

// 2. Use Case (Application Layer)
package com.jbh.finance.application.feature.product.usecases;

import com.jbh.finance.application.feature.product.dto.ProductDTO;

public class CreateProductUseCase implements CreateProductInputPort {

  public ProductDTO execute() {
    // Business logic returns DTO
    return new ProductDTO(...);
  }
}
```

## Dependency Rules

### Allowed Dependencies

```
✅ Application → Domain (business logic can use domain entities)
✅ Infrastructure → Application (adapters call use cases)
✅ Infrastructure → Domain (adapters can use domain entities)
```

### Forbidden Dependencies

```
❌ Domain → Application (domain must be pure)
❌ Domain → Infrastructure (domain must be framework-agnostic)
❌ Application → Infrastructure (application doesn't know about HTTP/DB)
```

## Benefits

### Separation of Concerns
- **DTOs**: Business data representation, can evolve with domain
- **Responses**: API contracts, must remain stable for clients
- **Entities**: Database representation, optimized for persistence

### Testability
- Application layer can be tested without HTTP/JSON
- Use cases work with DTOs, not framework-specific objects
- Mock repositories using port interfaces

### Flexibility
- Change API contracts without touching business logic
- Swap persistence layer without changing use cases
- Support multiple API versions by creating different Response objects from same DTOs

### Maintainability
- Clear boundaries prevent mixing concerns
- Feature organization makes navigation easy
- Infrastructure changes don't ripple into business logic
