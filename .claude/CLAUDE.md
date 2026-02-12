# General Instructions

- Always use Context7 MCP when I need library/API documentation, code generation, setup or configuration steps without me having to explicitly ask.
- Always generate unit tests for new code generated on any module different from jbh-z-assembly and jbh-*****-infra. The minimum coverage is 50%.

## PMD Rules

- Declare all variabels final as possible
- Avoid using literals in if statements.
- A method/constructor should not explicitly throw java.lang.Exception.
- Avoid catching generic exceptions such as NullPointerException, RuntimeException, Exception in try-catch block.

## Use Case Documentation Standard

All Use Case **interfaces** (in `*.application.core.usecases` package) MUST be documented with comprehensive JavaDoc following this structure:

### Class-Level Documentation

```java
/**
 * [Technical purpose - what this use case does]
 *
 * <p><strong>User Explanation:</strong> "[User-friendly explanation for FE display -
 * describe the action in simple terms as if explaining to end user]"
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>[Key business rule 1]
 *   <li>[Key business rule 2]
 *   <li>[Additional rules as needed]
 * </ul>
 */
```

### Method-Level Documentation

```java
/**
 * [Brief description of what the method does]
 *
 * <p><strong>Validations:</strong>
 *
 * <ul>
 *   <li>[Validation 1 - e.g., "Command cannot be null"]
 *   <li>[Validation 2 - e.g., "User must own the product"]
 *   <li>[Additional validations]
 * </ul>
 *
 * <p><strong>Database Operations:</strong>
 *
 * <ul>
 *   <li>INSERT: [Tables/entities created - e.g., "New movement record"]
 *   <li>UPDATE: [Tables/entities updated - e.g., "Product balance and net flow"]
 *   <li>DELETE: [Tables/entities deleted - e.g., "Sets deleted_at timestamp"]
 * </ul>
 *
 * @param [param] [description]
 * @return [description]
 * @throws BusinessException [when/why exception is thrown]
 */
```

### Important Notes

- Document ONLY the use case interface, NOT the implementation (InputPort classes)
- User Explanation should be written as if speaking directly to the end user
- Database Operations should list ALL database changes (INSERT/UPDATE/DELETE)
- For read-only queries, use "SELECT: [what is queried]" or "None (reads from registry)"
- Validations should include ALL checks performed by the use case

## Architecture Design

- [Architecture](agents/quarkus-multimodule-architect.md) - Quarkus Multimodule Architect
- [Code Design](docs/code-best-practices.md) - Code Best Practices

### Hexagonal Architecture - API Object Placement

**CRITICAL RULE:** Request and Response objects belong in the **infra** module, NOT the application module.

#### Module Responsibilities:

**Application Module (`jbh-xxx-application`):**

All application modules MUST follow this **feature-based structure**:

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

**Package Organization Rules:**

1. **Cross-Cutting Infrastructure** (acid/, async/, common/):
    - Technical services used across all features
    - Examples: UnitOfWork, AsyncTaskExecutor, LoggingContext
    - NEVER put business logic here

2. **Feature Packages** (feature/<feature-name>/):
    - Each feature is a vertical slice (bounded context)
    - Contains ALL business logic for that feature
    - Self-contained with clear boundaries
    - Microservices-ready (can be extracted easily)
    - Examples: `feature/product/`, `feature/movement/`, `feature/reporting/`

3. **Shared Business** (shared/):
    - Business-level code used by multiple features
    - Examples: BusinessException, CommandValidator
    - NOT for infrastructure concerns

**Feature Package Internal Structure:**

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

**Naming Conventions:**

- Feature packages: lowercase, singular (e.g., `product`, `movement`, `user`)
- Use case interfaces: `*InputPort` (e.g., `CreateProductInputPort`)
- Use case implementations: `*UseCase` (e.g., `CreateProductUseCase`)
- DTOs: `*DTO` (e.g., `ProductDTO`)
- Commands: `*Command` (e.g., `CreateProductCommand`)
- Mappers: `<Entity>Mapper` (e.g., `ProductMapper`)

**Module Exports (module-info.java):**

```java
// Export feature public APIs
exports com.jbh.xxx.application.feature .<feature>.dto;
exports com.jbh.xxx.application.feature .<feature>.ports.input;
exports com.jbh.xxx.application.feature .<feature>.usecases;

// Export shared public APIs
exports com.jbh.xxx.application.shared.exceptions;

// Do NOT export: services, validation, commands, mappers, ports.output
```

**Infrastructure Module (`jbh-xxx-infra`):**

**REST API Package Structure (Feature-Based Organization):**

The REST API layer is organized by **feature/bounded context** (vertical slices) rather than technical role:

```
com.jbh.products.infra.adapters.in.rest/
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

**Package Guidelines:**

- **Feature packages** (product/, movement/, balancehistory/, category/): Group related REST adapters with their contracts
- **request/**: API input contracts specific to the feature
- **response/**: API output contracts specific to the feature
- **common/**: Shared base classes and constants used across features

**Response Objects**:

- Package: `*.infra.adapters.in.rest.<feature>.response`
- Used only as controller method return types
- Map from internal DTOs using `fromDTO()` factory methods
- Examples: `ProductResponse`, `MonthlyBalanceResponse`
- **Naming Convention**: Must use `*Response` suffix, NEVER `*DTO`

**Request Objects**:

- Package: `*.infra.adapters.in.rest.<feature>.request`
- Used only as controller method parameters
- Examples: `CreateProductRequest`, `MonthlyBalanceRequest`
- **Naming Convention**: Must use `*Request` suffix

**Entities**: Database persistence objects

- Package: `*.infra.adapters.out.persistence.entity`
- Never exposed outside persistence layer
- Examples: `ProductEntity`, `MonthlyBalanceEntity`

#### Mapping Pattern:

```java
// In infra module - Response object
package com.jbh.products.infra.adapters.in.rest.product.response;

import dto.product.feature.com.jbh.finance.application.ProductDTO;

public record ProductResponse(...) {

  public static ProductResponse fromDTO(final ProductDTO dto) {
    return new ProductResponse(...);
  }
}

// In controller
package com.jbh.products.infra.adapters.in.rest.product;

import response.product.rest.in.adapters.com.jbh.finance.infra.ProductResponse;
import request.product.rest.in.adapters.com.jbh.finance.infra.CreateProductRequest;
import input.ports.product.feature.com.jbh.finance.application.CreateProductInputPort;

@Path("/products")
public class ProductRestAdapter {

  @GET
  public ProductResponse getProduct() {
    ProductDTO dto = service.getProduct(); // DTO from application feature layer
    return ProductResponse.fromDTO(dto);   // Convert to Response for API
  }
}
```

**Why feature-based organization?**

- **High cohesion**: Everything related to a feature is together
- **Easy navigation**: "I need product APIs? Go to `product/`"
- **Independent changes**: Modify one feature without touching others
- **Team ownership**: Teams can own entire vertical slices
- **Aligns with DDD**: Matches bounded contexts
- **Microservices-ready**: Each folder could become a separate service

**Why separate DTOs from Responses?**

- DTOs contain business logic and can change with domain requirements
- Response objects define stable API contracts for external consumers
- Infra layer handles all external communication (HTTP, DB, messaging)
- Application layer remains pure business logic, unaware of HTTP/JSON concerns

### Database Schema Guidelines

- **One schema per module**: Each module should use a single PostgreSQL schema
- **No multiple schemas within a module**: Avoid creating multiple schemas (e.g., userprefs + teamprefs) within a single module
- **Use natural primary keys**: When a natural unique identifier exists (user_id, team_id), use it as the primary key instead of creating a separate UUID id column

### Current Infrastructure

- **Existing Services:** `jbh-gateway`, `jbh-consul-service-discovery`, `jbh-iam-service` (user authentication)
- **Target Architecture:** Single deployable unit with well-defined modules
- **Database:** PostgreSQL with separate schema per module
- **Service Discovery:** Consul (running on port 8500)
- **Authentication:** JWT-based authorization for all HTTP requests and inter-module communication

### Deployment

- **Budget Constraint:** Cost-effective hosting solutions only (no cloud provider dependencies)
- **Deployment Model:** Single unit deployment with multiple modules
- **Monitoring:** Centralized logging and monitoring with ELK stack and Prometheus/Grafana
- **Tracing:** Distributed tracing capability with Jaeger or Zipkin

### Technology Stack

- **Runtime:** Quarkus with GraalVM support for native compilation
- **Build Tool:** Maven with multi-module configuration
- **Database:** PostgreSQL with schema-per-module approach
- **Security:** JWT with RS256 signing
- **Service Discovery:** Consul integration
- **API Gateway:** Quarkus-based routing

### Jandex Indexing Strategy (Hexagonal Architecture Compliance)

**CRITICAL:** Maintain framework-agnostic domain/application layers while enabling Quarkus CDI and reflection.

#### Two-Tier Indexing Approach:

**1. Infrastructure Modules (`*-infra`) - Jandex Plugin:**

```xml
<!-- In jbh-xxx-infra/pom.xml -->
<plugin>
  <groupId>io.smallrye</groupId>
  <artifactId>jandex-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>make-index</id>
      <goals>
        <goal>jandex</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

**Purpose:** Index CDI beans for runtime discovery:

- REST endpoints (`@Path`, `@GET`, etc.)
- Services (`@ApplicationScoped`, `@RequestScoped`)
- Repositories, adapters, and infrastructure components

**Result:** Each infra JAR contains `META-INF/jandex.idx`

**2. Domain/Application Modules - `quarkus.index-dependency` Configuration:**

```properties
# In jbh-z-assembly/src/main/resources/application.properties
quarkus.index-dependency.products-domain.group-id=com.jbh
quarkus.index-dependency.products-domain.artifact-id=jbh-products-domain
quarkus.index-dependency.products-application.group-id=com.jbh
quarkus.index-dependency.products-application.artifact-id=jbh-products-application
# ... repeat for each domain/application module
```

**Purpose:** Index non-CDI classes for OpenAPI/Swagger and reflection:

- DTOs (Data Transfer Objects)
- VOs (Value Objects)
- Domain entities
- Command objects

**Result:** Domain/application modules remain framework-agnostic (NO Quarkus dependencies)

#### Why This Matters:

| Aspect                     | Benefit                                                           |
|----------------------------|-------------------------------------------------------------------|
| **Hexagonal Architecture** | Domain/application layers have ZERO infrastructure dependencies   |
| **Framework Independence** | Can switch from Quarkus to Spring without touching business logic |
| **Clean Architecture**     | Infrastructure concerns isolated to infra/assembly layers         |
| **CDI Discovery**          | Infra beans properly discovered at runtime                        |
| **OpenAPI Generation**     | DTOs/VOs indexed for Swagger documentation                        |
| **Native Compilation**     | All classes properly registered for GraalVM reflection            |

#### Module Dependency Rules:

**❌ NEVER allow in domain/application modules:**

- Quarkus dependencies (`io.quarkus.*`)
- Spring dependencies (`org.springframework.*`)
- JAX-RS annotations (`jakarta.ws.rs.*`)
- CDI annotations in domain objects
- Any framework-specific code

**✅ ONLY allow in domain/application modules:**

- JDK standard library
- Domain-specific libraries (validation, money types)
- SLF4J API (logging facade only)
- Test dependencies (JUnit, Mockito)

**✅ Infrastructure dependencies belong in:**

- `*-infra` modules (Quarkus, JAX-RS, Hibernate, etc.)
- `jbh-z-assembly` module (Quarkus bootstrap, configuration)