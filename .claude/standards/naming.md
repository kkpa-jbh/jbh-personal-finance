# Naming Conventions

This document defines **strict naming conventions** for backend classes to ensure clear separation of concerns, stable API contracts, and maintainable architecture.

---

## 1. Persistence Layer (Database / ORM)

### Purpose
- Represents database tables
- Used only for persistence
- Must never be exposed outside the persistence layer

### Naming Convention
```
<EntityName>JPAEntity
```

### Examples
- `MovementJPAEntity`
- `MonthlyBalanceEntity`
- `UserEntity`
- `TransactionEntity`
- `ProductEntity`

### Package Location
```
*.infra.adapters.out.persistence.<feature>/
```

### Rules
- Used only by repositories
- Contains ORM annotations (`@Entity`, `@Table`, `@Column`)
- Never returned by API controllers
- Must include `toDTO()` method to map to DTOs

---

## 2. Internal Data Transfer Objects (Application Layer)

### Purpose
- Used for internal data transfer between layers
- Represents domain-level data
- May contain business-related fields

### Naming Convention
```
<EntityName>DTO
```

### Examples
- `MovementDTO`
- `MonthlyBalanceDTO`
- `UserDTO`
- `ProductDTO`

### Package Location
```
*.application.feature.<feature>.dto/
```

### Rules
- Internal use only
- Must not be serialized directly to API responses
- Free to change as business logic evolves
- Can contain business logic methods
- NEVER exposed in REST controller return types

---

## 3. API Request Objects (Inbound API)

### Purpose
- Represents data received from API clients
- Defines the input API contract
- Only present in the infra modules

### Naming Convention
```
<ActionName><EntityName>Request
```

### Examples
- `AddMovementRequest`
- `CreateProductRequest`
- `EditProductRequest`
- `MonthlyBalanceRequest`
- `CreateUserRequest`
- `UpdateProductMetadataRequest`

### Package Location
```
*.infra.adapters.in.rest.<feature>.request/
```

### Rules
- Used only in API controllers as method parameters
- Validated at the API boundary
- Never reused as domain or persistence models
- Transforms into Command objects for use case execution

---

## 4. API Response Objects (Outbound API)

### Purpose
- Represents data returned to API clients
- Defines the public API contract
- Optimized for client consumption
- Only present in the infra modules

### Naming Convention
```
<EntityName>Response

// or when needed
<EntityName>ApiResponse
```

### Examples
- `MovementResponse`
- `ProductResponse`
- `MonthlyBalanceResponse`
- `UserResponse`
- `AddBasicMovementResponse`
- `ProductTypeResponse`
- `CategoryResponse`

### Package Location
```
*.infra.adapters.in.rest.<feature>.response/
```

### Rules
- Must NEVER be named `DTO`
- Must NEVER expose Entity classes
- Can differ in structure from internal DTOs
- Must be stable and versionable
- Used only as controller method return types
- Map from internal DTOs using `fromDTO()` factory methods

---

## 5. Use Case Components

### Use Case Interface
**Naming:** `<Action><Entity>UseCase`

**Examples:**
- `AddMovementUseCase`
- `CreateProductUseCase`
- `FindMovementsUseCase`
- `GetMonthlyBalanceUseCase`

**Package Location:**
```
*.application.feature.<feature>.usecases/
```

### Input Port Implementation
**Naming:** `<Action><Entity>InputPort`

**Examples:**
- `AddMovementInputPort`
- `CreateProductInputPort`
- `FindMovementsInputPort`

**Package Location:**
```
*.application.feature.<feature>.ports.input/
```

**Rules:**
- Implements the corresponding UseCase interface
- NO CDI annotations on class or constructor
- Manual constructor injection
- All fields must be `final`

### Output Port Interface
**Naming:** `<Entity><Query|Writer>Repository`

**Examples:**
- `AccountMovementQueryRepository`
- `ProductWriterRepository`
- `UserQueryRepository`

**Package Location:**
```
*.application.feature.<feature>.ports.output/
```

---

## 6. Command Objects

### Naming Convention
```
<Action><Entity>Command
```

### Examples
- `AddMovementCommand`
- `CreateProductCommand`
- `UpdateProductMetadataCommand`
- `EditProductCommand`

### Package Location
```
*.application.feature.<feature>.commands/
```

### Rules
- Records with validation methods
- Include `validate()` method
- Provide builder via static factory method
- Immutable by design

---

## 7. Services

### Application Services
**Naming:** `<Feature>ApplicationService` or `<Entity>Service`

**Examples:**
- `MovementApplicationService`
- `ProductsService`
- `UserService`

**Package Location:**
```
*.application.feature.<feature>.services/
```

### Service Implementations
**Naming:** `<Entity>ServiceImpl` or `<Feature>Service`

**Examples:**
- `AccountMovementServiceImpl`
- `ProductServiceImpl`

---

## 8. REST Adapters

### Naming Convention
```
<Feature>RestAdapter
```

### Examples
- `MovementRestAdapter`
- `ProductRestAdapter`
- `MonthlyBalanceRestAdapter`
- `TransferRestAdapter`
- `ExpenseCategoryRestAdapter`
- `IncomeCategoryRestAdapter`

### Package Location
```
*.infra.adapters.in.rest.<feature>/
```

### Rules
- Annotated with `@RequestScoped`
- Annotated with `@Path`
- Inject UseCase interfaces (NOT InputPorts directly)
- Constructor injection with `@Inject`
- Extends `BaseRestAdapter` for common functionality

---

## 9. Repository Adapters

### Naming Convention
```
<Entity>Repository<Query|Writer>Adapter
```

### Examples
- `AccountMovementRepositoryQueryAdapter`
- `ProductRepositoryWriterAdapter`

### Package Location
```
*.infra.adapters.out.persistence.<feature>/
```

### Rules
- Annotated with `@ApplicationScoped`
- Implements output port interfaces
- Delegates to JPA repositories

---

## 10. JPA Repositories

### Naming Convention
```
<Entity>JPARepository
```

### Examples
- `AccountMovementJPARepository`
- `ProductJPARepository`

### Package Location
```
*.infra.adapters.out.persistence.<feature>/
```

### Rules
- Annotated with `@ApplicationScoped`
- Annotated with `@PersistenceUnit(name = "finance")`
- Implements `PanacheRepository<EntityType>`

---

## 11. Mappers

### Naming Convention
```
<Entity>Mapper
```

### Examples
- `ProductMapper`
- `MovementMapper`
- `UserMapper`

### Package Location
```
*.application.feature.<feature>.mappers/
```

### Rules
- Static utility methods for Domain ↔ DTO mapping
- No instantiation (private constructor)

---

## 12. Package Naming Conventions

### Feature Packages
- **Format:** lowercase, singular
- **Examples:** `product`, `movement`, `user`, `balancehistory`, `category`

### REST API Feature Packages
```
com.jbh.finance.infra.adapters.in.rest/
├── product/
├── movement/
├── balancehistory/
├── category/
└── common/
```

---

## 13. Method Naming Patterns

### Use Case Methods
- Action verbs: `add`, `create`, `update`, `delete`, `find`, `get`, `calculate`
- **Examples:**
  - `addMovement()`
  - `createProduct()`
  - `findByUserAndProductId()`

### Repository Methods
- Query prefix: `findBy*`, `getBy*`, `exists*`, `count*`
- Mutation prefix: `save*`, `update*`, `delete*`
- **Examples:**
  - `findByAccountId()`
  - `saveMovement()`
  - `deleteById()`

---

## 14. Mandatory Mapping Flow

All backend data transformations must follow this flow:

```
<EntityName>JPAEntity
        ↓
<EntityName>DTO
        ↓
<EntityName>Response
```

Skipping layers is not allowed unless explicitly instructed.

---

## 15. Forbidden Patterns ❌

The following are **NOT allowed**:
- Returning `*DTO` from API controllers
- Returning `*Entity` from API controllers
- Naming API responses with `DTO`
- Mixing persistence and API concerns in the same class
- Using generic names like `Data`, `Info`, `Object`

---

## 16. Enforcement Rules

| Layer | Uses |
|-------|------|
| Controllers | `*Request` and `*Response` |
| Use Cases / Input Ports | `*Command` and `*DTO` |
| Services | `*DTO` |
| Repositories | `*JPAEntity` |

When unclear, assume full separation of layers.

---

## 17. Correct Example

```
# Domain/Application
MovementDTO
AddMovementCommand
AddMovementUseCase (interface)
AddMovementInputPort (implementation)
AccountMovementQueryRepository (output port interface)
MovementApplicationService

# Infrastructure
MovementJPAEntity
MovementJPARepository
AccountMovementRepositoryQueryAdapter
MovementRestAdapter
AddMovementRequest
MovementResponse
```

---

## Key Principles

**DTOs are internal.**
**Responses are contracts.**
**Entities are persistence-only.**

**Use Cases define behavior.**
**Input Ports implement behavior.**
**Output Ports define dependencies.**
