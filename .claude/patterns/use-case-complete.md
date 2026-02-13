# Use Case Architecture Patterns - JBH Finance Module

> **Purpose:** Memory file for building complete use cases in the jbh-finance modular monolith
> **Last Updated:** 2026-02-12

## 📁 Module Structure

```
jbh-finance/
├── jbh-finance-domain/          # Domain entities and value objects
├── jbh-finance-application/     # Business logic (ports, use cases, services)
└── jbh-finance-infra/          # Infrastructure (REST, persistence)
```

## 🎯 Use Case Implementation Pattern

### 1. Use Case Interface (Application Module)
**Location:** `*.application.feature.<feature>.usecases/`
**Naming:** `<Action><Entity>UseCase` (e.g., `AddMovementUseCase`, `FindMovementsUseCase`)
**Purpose:** Define the contract with full JavaDoc documentation

```java
package com.jbh.finance.application.feature.movement.usecases;

/**
 * [Technical purpose - what this use case does]
 *
 * <p><strong>User Explanation:</strong> "[User-friendly explanation]"
 *
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>[Rule 1]
 *   <li>[Rule 2]
 * </ul>
 */
public interface AddMovementUseCase {

  /**
   * [Brief description]
   *
   * <p><strong>Validations:</strong>
   * <ul>
   *   <li>User ID cannot be null
   *   <li>Command validation rules
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   * <ul>
   *   <li>INSERT: New movement record
   *   <li>UPDATE: Product balance
   * </ul>
   *
   * @param userId the user ID
   * @param productId the product ID
   * @param command the command object
   * @return DTO with result
   * @throws BusinessException if validation fails
   */
  AddBasicMovementDTO addMovement(UUID userId, ProductId productId, AddMovementCommand command)
      throws BusinessException;
}
```

### 2. Input Port Implementation (Application Module)
**Location:** `*.application.feature.<feature>.ports.input/`
**Naming:** `<Action><Entity>InputPort` (e.g., `AddMovementInputPort`)
**Purpose:** Implements the use case interface with business logic orchestration

```java
package com.jbh.finance.application.feature.movement.ports.input;

public class AddMovementInputPort implements AddMovementUseCase {

  private final MovementApplicationService movementService;
  private final ProductsService productsService;

  // Constructor injection (NO @Inject - wired manually in CDI config)
  public AddMovementInputPort(
      final MovementApplicationService movementService,
      final ProductsService productsService) {
    this.movementService = movementService;
    this.productsService = productsService;
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final UUID userId,
      final ProductId productId,
      final AddMovementCommand command)
      throws BusinessException {

    // 1. Null validations
    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    // 2. Command validation
    command.validate();

    // 3. Business logic via services
    final ProductDTO product = productsService.findByUserAndProductId(userId, productId);

    // 4. Delegate to service
    return movementService.addMovementProcessingBalances(
        new ProductPK(userId, productId), command);
  }
}
```

**Key Points:**
- NO annotations on class or constructor
- All final fields
- Services injected via constructor
- Validation first, then business logic
- Delegates complex logic to services

### 3. Output Port Interface (Application Module)
**Location:** `*.application.feature.<feature>.ports.output/`
**Naming:** `<Entity><Query|Writer>Repository`
**Purpose:** Define repository contract (implemented in infra)

```java
package com.jbh.finance.application.feature.movement.ports.output;

public interface AccountMovementQueryRepository {
  List<MovementDTO> findByAccountId(ProductId accountId);
  List<MovementDTO> findByUserAndProductId(UUID userId, ProductId productId, LocalDate startDate, LocalDate endDate);
}
```

### 4. DTO (Application Module)
**Location:** `*.application.feature.<feature>.dto/`
**Naming:** `<Entity>DTO`
**Purpose:** Data transfer between layers

```java
package com.jbh.finance.application.feature.movement.dto;

import lombok.Builder;

@Builder
public record MovementDTO(
    MovementId id,
    ProductId accountId,
    MovementType movementType,
    MovementCategoryVO category,
    BigDecimal movementAmount,
    LocalDate movementDate,
    BigDecimal balanceSnapshot,
    AccountMovementMetadata metadata,
    LocalDateTime createdAt,
    String description) {

  // Optional: Business logic methods
  public boolean isWithdrawalType() {
    return movementType.isWithdrawal();
  }
}
```

### 5. Command Object (Application Module)
**Location:** `*.application.feature.<feature>.commands/`
**Naming:** `<Action><Entity>Command`
**Purpose:** Input data with validation

```java
package com.jbh.finance.application.feature.movement.commands;

/**
 * JavaDoc with usage examples
 */
public record AddMovementCommand(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot,
    MovementType movementType,
    MovementCategoryVO categoryDTO,
    String description) {

  public static MovementCommandBuilder builder() {
    return new MovementCommandBuilder();
  }

  public void validate() {
    if (entryDate == null) {
      throw new IllegalArgumentException("Entry date cannot be null");
    }
    // More validations...
  }

  // Custom builder class (not Lombok @Builder)
  public static final class MovementCommandBuilder {
    // Builder implementation...
  }
}
```

### 6. Repository Adapter (Infra Module)
**Location:** `*.infra.adapters.out.persistence.<feature>/`
**Naming:** `<Entity>RepositoryQueryAdapter`

```java
package com.jbh.finance.infra.adapters.out.persistence.movement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AccountMovementRepositoryQueryAdapter implements AccountMovementQueryRepository {

  @Inject AccountMovementJPARepository jpaRepo;

  @Override
  public List<MovementDTO> findByAccountId(final ProductId accountId) {
    return jpaRepo.findByAccountId(accountId.value());
  }
}
```

### 7. JPA Repository (Infra Module)
**Location:** `*.infra.adapters.out.persistence.<feature>/`
**Naming:** `<Entity>JPARepository`

```java
package com.jbh.finance.infra.adapters.out.persistence.movement;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;

@ApplicationScoped
@PersistenceUnit(name = "finance")
public class AccountMovementJPARepository implements PanacheRepository<MovementJPAEntity> {

  private static final String ACCOUNT_ID_PARAM = "accountId";

  public List<MovementDTO> findByAccountId(final UUID accountId) {
    return find("accountId = :accountId", Parameters.with(ACCOUNT_ID_PARAM, accountId))
        .list()
        .stream()
        .map(MovementJPAEntity::toDTO)
        .toList();
  }
}
```

### 8. JPA Entity (Infra Module)
**Location:** `*.infra.adapters.out.persistence.<feature>/`
**Naming:** `<Entity>JPAEntity`

```java
@Entity
@Getter
@Setter
@Table(name = "movements", schema = "finance")
public class MovementJPAEntity extends PanacheEntityBase {
  @Id
  @Column(name = "id")
  public UUID id;

  @Column(name = "product_id")
  public UUID accountId;

  // More fields...

  public MovementDTO toDTO() {
    return MovementDTO.builder()
        .id(MovementId.of(id))
        .accountId(ProductId.of(accountId))
        .movementType(movementType)
        // Map all fields...
        .build();
  }
}
```

### 9. REST Adapter (Infra Module)
**Location:** `*.infra.adapters.in.rest.<feature>/`
**Naming:** `<Feature>RestAdapter`

```java
package com.jbh.finance.infra.adapters.in.rest.movement;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path(FinanceApiRoutes.MOVEMENTS_API_PATH)
@Tag(name = "Movement Operations")
public class MovementRestAdapter extends BaseRestAdapter {

  private final AddMovementUseCase addMovementUseCase; // Inject UseCase interface!

  @Inject
  public MovementRestAdapter(final AddMovementUseCase addMovementUseCase) {
    this.addMovementUseCase = addMovementUseCase;
  }

  @POST
  @Path("/{productId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Add movement")
  public Response addMovement(
      @PathParam("productId") final UUID productId,
      @RequestBody final AddMovementRequest request,
      @HeaderParam("Authorization") final String authHeader) {

    final UUID userId = findUserId(authHeader); // From BaseRestAdapter

    // 1. Build command from request
    final AddMovementCommand command = new AddMovementCommand(...);

    // 2. Call use case
    final AddBasicMovementDTO dto = addMovementUseCase.addMovement(userId, ProductId.of(productId), command);

    // 3. Transform DTO to Response
    return Response.ok(AddBasicMovementResponse.fromDTO(dto)).build();
  }
}
```

### 10. Request Object (Infra Module)
**Location:** `*.infra.adapters.in.rest.<feature>.request/`
**Naming:** `<Action><Entity>Request`

```java
package com.jbh.finance.infra.adapters.in.rest.movement.request;

public record AddMovementRequest(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot,
    MovementType movementType,
    String categoryName,
    String description) {}
```

### 11. Response Object (Infra Module)
**Location:** `*.infra.adapters.in.rest.<feature>.response/`
**Naming:** `<Entity>Response` (NOT `<Entity>DTO`!)

```java
package com.jbh.finance.infra.adapters.in.rest.movement.response;

public record MovementResponse(
    MovementId id,
    ProductId accountId,
    MovementType movementType,
    // All fields...
    ) {

  public static MovementResponse fromDTO(final MovementDTO dto) {
    return new MovementResponse(
        dto.id(),
        dto.accountId(),
        dto.movementType(),
        // Map all fields...
    );
  }
}
```

### 12. CDI Configuration (Infra Module)
**Location:** `*.infra/ProductUseCasesCDIConfig.java`

```java
@ApplicationScoped
public class ProductUseCasesCDIConfig {

  @Inject AccountMovementQueryRepository accountMovementQueryRepo;
  @Inject ProductsService productsService;

  @Produces
  @ApplicationScoped
  public AddMovementUseCase registeringAddMovementUseCase() {
    // Return InputPort as UseCase interface type
    return new AddMovementInputPort(accountMovementService(), productsService);
  }

  @Produces
  public MovementService accountMovementService() {
    return new AccountMovementServiceImpl(accountMovementQueryRepo);
  }
}
```

## 🔑 Key Conventions

### Naming Conventions
- **Use Case Interface:** `<Action><Entity>UseCase`
- **Input Port:** `<Action><Entity>InputPort`
- **Output Port:** `<Entity><Query|Writer>Repository`
- **DTO:** `<Entity>DTO`
- **Command:** `<Action><Entity>Command`
- **REST Adapter:** `<Feature>RestAdapter`
- **Request:** `<Action><Entity>Request`
- **Response:** `<Entity>Response` (NOT DTO!)
- **JPA Entity:** `<Entity>JPAEntity`
- **JPA Repository:** `<Entity>JPARepository`
- **Repository Adapter:** `<Entity>Repository<Query|Writer>Adapter`

### Package Organization (Feature-Based)
```
com.jbh.finance.application.feature.<feature>/
├── dto/                    # MovementDTO
├── commands/               # AddMovementCommand
├── ports/
│   ├── input/             # AddMovementInputPort
│   └── output/            # AccountMovementQueryRepository
├── usecases/              # AddMovementUseCase (interface)
├── services/              # MovementService, MovementServiceImpl
├── mappers/               # Domain ↔ DTO mappers
└── validation/            # Feature-specific validators
```

### PMD Rules
- All variables FINAL when possible
- NO literals in if statements
- NO generic exceptions (Exception, RuntimeException, NullPointerException)
- NO explicit throws of java.lang.Exception

### Test Coverage
- Minimum 50% coverage
- Tests for all modules except `jbh-z-assembly` and `*-infra`

## 🎨 Design Principles

1. **Hexagonal Architecture:**
   - Domain/Application modules: NO framework dependencies
   - Infra module: All framework code (Quarkus, JAX-RS, JPA)

2. **Dependency Flow:**
   - Domain ← Application ← Infra
   - Infra depends on Application (via ports)
   - Application NEVER depends on Infra

3. **DTO vs Response:**
   - DTOs: Application layer (business data)
   - Responses: Infra layer (API contracts)
   - ALWAYS transform DTO → Response in REST adapter

4. **Constructor Injection:**
   - InputPorts: Manual constructor injection (NO @Inject)
   - REST Adapters: CDI injection (@Inject on constructor)
   - Repository Adapters: CDI injection (@Inject on fields)

5. **Use Case Responsibilities:**
   - Input validation
   - Business rule orchestration
   - Delegation to services
   - Transaction boundaries (via UnitOfWork if needed)

6. **Service Responsibilities:**
   - Complex business logic
   - Multi-repository operations
   - Domain entity manipulation

## 📊 Complete Use Case Checklist

- [ ] Use Case Interface (with full JavaDoc)
- [ ] Input Port Implementation
- [ ] Command Object (with validation)
- [ ] DTO (if new entity)
- [ ] Output Port Interface (if repository needed)
- [ ] Service (if business logic needed)
- [ ] Repository Adapter (Infra)
- [ ] JPA Repository (Infra)
- [ ] JPA Entity (Infra, if new table)
- [ ] REST Adapter (Infra)
- [ ] Request Object (Infra)
- [ ] Response Object (Infra)
- [ ] CDI Producer Method
- [ ] Unit Tests (50%+ coverage)
- [ ] Integration Tests

## 🚀 Value Objects Pattern

Domain VOs are immutable wrappers with factory methods:

```java
public record ProductId(UUID value) {
  public static ProductId of(UUID value) {
    return new ProductId(value);
  }
}
```

Usage:
```java
ProductId.of(uuid)      // Create
productId.value()       // Access
```
