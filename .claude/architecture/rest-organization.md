# REST API Organization

## Overview

The REST API layer in the infrastructure module follows **feature-based organization**, aligning with the application module's structure. This creates vertical slices from the API layer down to the database.

## REST API Package Structure

```
com.jbh.finance.infra.adapters.in.rest/
├── product/
│   ├── ProductRestAdapter.java
│   ├── ProductConfigRestAdapter.java
│   ├── request/
│   │   ├── CreateProductRequest.java
│   │   ├── EditProductRequest.java
│   │   └── UpdateProductMetadataRequest.java
│   └── response/
│       ├── ProductResponse.java
│       └── ProductTypeResponse.java
├── movement/
│   ├── MovementRestAdapter.java
│   ├── TransferRestAdapter.java
│   ├── request/
│   │   ├── AddMovementRequest.java
│   │   └── AddTransferRequest.java
│   └── response/
│       ├── MovementResponse.java
│       └── AddBasicMovementResponse.java
├── balancehistory/
│   ├── MonthlyBalanceRestAdapter.java
│   ├── request/
│   │   └── MonthlyBalanceRequest.java
│   └── response/
│       └── MonthlyBalanceResponse.java
├── category/
│   ├── ExpenseCategoryRestAdapter.java
│   ├── IncomeCategoryRestAdapter.java
│   └── response/
│       └── CategoryResponse.java
└── common/
    ├── BaseRestAdapter.java
    ├── FinanceApiRoutes.java
    └── ApiConstants.java
```

## Package Guidelines

### Feature Packages
Group related REST adapters with their contracts.

**Examples**: `product/`, `movement/`, `balancehistory/`, `category/`

**Contains**:
- REST adapter classes (controllers)
- `request/` subdirectory for API inputs
- `response/` subdirectory for API outputs

### request/ Subdirectory
API input contracts specific to the feature.

**Purpose**: Define what data the API accepts from clients

**Examples**: `CreateProductRequest`, `MonthlyBalanceRequest`

**Naming Convention**: Must use `*Request` suffix

### response/ Subdirectory
API output contracts specific to the feature.

**Purpose**: Define what data the API returns to clients

**Examples**: `ProductResponse`, `MonthlyBalanceResponse`

**Naming Convention**: Must use `*Response` suffix, NEVER `*DTO`

### common/ Subdirectory
Shared base classes and constants used across features.

**Contains**:
- `BaseRestAdapter`: Common controller functionality
- `FinanceApiRoutes`: API path constants
- `ApiConstants`: Shared constants

## Response Objects

### Characteristics

- **Package**: `*.infra.adapters.in.rest.<feature>.response`
- **Usage**: Controller method return types ONLY
- **Mapping**: Convert from DTOs using `fromDTO()` factory methods
- **Naming**: Must use `*Response` suffix

### Example

```java
package com.jbh.finances.infra.adapters.in.rest.product.response;

import com.jbh.finance.application.feature.product.dto.ProductDTO;

public record ProductResponse(
    String id,
    String name,
    String type
) {
  public static ProductResponse fromDTO(final ProductDTO dto) {
    return new ProductResponse(
        dto.id(),
        dto.name(),
        dto.type()
    );
  }
}
```

## Request Objects

### Characteristics

- **Package**: `*.infra.adapters.in.rest.<feature>.request`
- **Usage**: Controller method parameters ONLY
- **Validation**: Can include JAX-RS validation annotations
- **Naming**: Must use `*Request` suffix

### Example

```java
package com.jbh.finances.infra.adapters.in.rest.product.request;

import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
    @NotNull String name,
    @NotNull String type
) {}
```

## REST Adapter Pattern

### Complete Example

```java
package com.jbh.finances.infra.adapters.in.rest.product;

import com.jbh.finance.infra.adapters.in.rest.product.response.ProductResponse;
import com.jbh.finance.infra.adapters.in.rest.product.request.CreateProductRequest;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductRestAdapter {

  @Inject
  CreateProductInputPort createProduct;

  @POST
  public ProductResponse create(final CreateProductRequest request) {
    // 1. Call use case with request data
    ProductDTO dto = createProduct.execute(request);

    // 2. Convert DTO to Response for API
    return ProductResponse.fromDTO(dto);
  }

  @GET
  @Path("/{id}")
  public ProductResponse getById(@PathParam("id") final String id) {
    // DTO from application layer
    ProductDTO dto = findProduct.execute(id);

    // Convert to Response for API
    return ProductResponse.fromDTO(dto);
  }
}
```

## Why Feature-Based Organization?

### 1. High Cohesion
Everything related to a feature's API is together in one place.

**Example**: All product-related endpoints, requests, and responses in `product/`

### 2. Easy Navigation
Developers can quickly find what they need.

**Question**: "Where are the product APIs?"
**Answer**: "In `adapters.in.rest.product/`"

### 3. Independent Changes
Modify one feature's API without touching others.

**Example**: Change movement endpoints without affecting product endpoints

### 4. Team Ownership
Teams can own entire vertical slices from API to database.

**Example**: Product team owns `product/` REST package + application feature + database schema

### 5. Aligns with DDD
REST organization matches bounded contexts in the domain.

**Example**: `product` bounded context → `feature.product` package → `rest.product` package

### 6. Microservices-Ready
Each feature folder could become a separate microservice.

**Example**: Extract `movement/` into a standalone microservice without massive refactoring

## Anti-Pattern: Technical Organization

### DON'T Organize by Technical Role

```
// ❌ BAD: Organized by technical role
com.jbh.finance.infra.adapters.in.rest/
├── request/
│   ├── CreateProductRequest.java
│   ├── AddMovementRequest.java
│   └── MonthlyBalanceRequest.java
├── response/
│   ├── ProductResponse.java
│   ├── MovementResponse.java
│   └── MonthlyBalanceResponse.java
└── controller/
    ├── ProductController.java
    ├── MovementController.java
    └── BalanceController.java
```

**Problems**:
- Low cohesion (related code scattered)
- Difficult navigation (hunt across packages)
- Tight coupling (changes affect multiple features)
- No clear feature boundaries

### DO Organize by Feature

```
// ✅ GOOD: Organized by feature
com.jbh.finance.infra.adapters.in.rest/
├── product/
│   ├── ProductRestAdapter.java
│   ├── request/
│   └── response/
├── movement/
│   ├── MovementRestAdapter.java
│   ├── request/
│   └── response/
└── balancehistory/
    ├── MonthlyBalanceRestAdapter.java
    ├── request/
    └── response/
```

**Benefits**:
- High cohesion (everything together)
- Easy navigation (clear structure)
- Loose coupling (features independent)
- Clear boundaries (aligned with domain)
