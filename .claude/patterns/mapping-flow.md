# Entity → DTO → Response Mapping Flow

## Overview
This pattern defines the data transformation flow in our hexagonal architecture, ensuring clean separation between layers.

## The Three-Layer Mapping

```
JPA Entity (Infra/Persistence)
    ↓ toDTO()
DTO (Application/Feature)
    ↓ fromDTO()
Response (Infra/REST)
```

## 1. JPA Entity → DTO (Infra to Application)

**Location:** Inside JPA Entity class (`*.infra.adapters.out.persistence.<feature>/`)

```java
@Entity
@Table(name = "products", schema = "finance")
public class ProductJPAEntity extends PanacheEntityBase {
  @Id
  public UUID id;
  public String name;
  public ProductType type;

  // Entity → DTO mapping
  public ProductDTO toDTO() {
    return ProductDTO.builder()
        .id(ProductId.of(id))
        .name(name)
        .type(type)
        .build();
  }
}
```

## 2. DTO → Response (Application to REST API)

**Location:** Inside Response class (`*.infra.adapters.in.rest.<feature>.response/`)

```java
package com.jbh.finance.infra.adapters.in.rest.product.response;

import com.jbh.finance.application.feature.product.dto.ProductDTO;

public record ProductResponse(
    ProductId id,
    String name,
    ProductType type) {

  // DTO → Response mapping
  public static ProductResponse fromDTO(final ProductDTO dto) {
    return new ProductResponse(
        dto.id(),
        dto.name(),
        dto.type()
    );
  }
}
```

## 3. Complete Flow in REST Adapter

**Location:** `*.infra.adapters.in.rest.<feature>/`

```java
package com.jbh.finance.infra.adapters.in.rest.product;

import com.jbh.finance.infra.adapters.in.rest.product.response.ProductResponse;
import com.jbh.finance.infra.adapters.in.rest.product.request.CreateProductRequest;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;

@Path("/products")
public class ProductRestAdapter {

  @Inject
  CreateProductInputPort createProductUseCase;

  @GET
  @Path("/{id}")
  public ProductResponse getProduct(@PathParam("id") UUID id) {
    // Use case returns DTO from application layer
    ProductDTO dto = createProductUseCase.getProduct(id);

    // Convert DTO to Response for API contract
    return ProductResponse.fromDTO(dto);
  }
}
```

## Key Principles

1. **JPA Entity**: Contains `toDTO()` method to convert persistence model to business model
2. **DTO**: Lives in application layer, represents business data
3. **Response**: Contains `fromDTO()` static factory to convert business model to API contract
4. **Request**: Input from REST API, converted to Command objects in REST adapter

## Naming Convention Rules

- JPA Entities: `<Entity>JPAEntity`
- DTOs: `<Entity>DTO` (in application module)
- Responses: `<Entity>Response` (NEVER `<Entity>DTO` in infra!)
- Requests: `<Action><Entity>Request`

## Why This Separation?

- **DTOs** contain business logic and can change with domain requirements
- **Response objects** define stable API contracts for external consumers
- **JPA Entities** are persistence implementation details
- Each layer has its own data representation, ensuring independence and flexibility
