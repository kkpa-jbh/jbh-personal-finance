# Code Quality Standards

This document defines code quality rules, PMD enforcement, SOLID principles, and best practices for the codebase.

---

## PMD Rules

These rules are **mandatory** and must be followed in all code:

### 1. Final Variables
**Rule:** Declare all variables `final` when possible

**Rationale:**
- Prevents accidental reassignment
- Makes code more predictable
- Improves readability and maintainability

**Examples:**

```java
// ❌ BAD
public void processMovement(AddMovementCommand command) {
  UUID userId = command.userId();
  BigDecimal amount = command.amount();
  amount = amount.multiply(BigDecimal.valueOf(1.1)); // Reassignment allowed
}

// ✅ GOOD
public void processMovement(final AddMovementCommand command) {
  final UUID userId = command.userId();
  final BigDecimal baseAmount = command.amount();
  final BigDecimal finalAmount = baseAmount.multiply(BigDecimal.valueOf(1.1));
}
```

### 2. No Literals in If Statements
**Rule:** Avoid using literals (magic numbers/strings) in if statements

**Rationale:**
- Improves readability
- Makes constants reusable
- Easier to maintain

**Examples:**

```java
// ❌ BAD
if (movementType.equals("WITHDRAWAL")) {
  // process withdrawal
}

if (amount > 1000) {
  // validate large amount
}

// ✅ GOOD
private static final String WITHDRAWAL_TYPE = "WITHDRAWAL";
private static final BigDecimal LARGE_AMOUNT_THRESHOLD = BigDecimal.valueOf(1000);

if (movementType.equals(WITHDRAWAL_TYPE)) {
  // process withdrawal
}

if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
  // validate large amount
}
```

### 3. No Explicit java.lang.Exception
**Rule:** A method/constructor should not explicitly throw `java.lang.Exception`

**Rationale:**
- Too generic and hides specific error scenarios
- Forces callers to catch overly broad exceptions
- Violates the principle of specific exception handling

**Examples:**

```java
// ❌ BAD
public void addMovement(final AddMovementCommand command) throws Exception {
  // implementation
}

// ✅ GOOD
public void addMovement(final AddMovementCommand command)
    throws BusinessException, ValidationException {
  // implementation
}
```

### 4. No Generic Exception Catching
**Rule:** Avoid catching generic exceptions such as `NullPointerException`, `RuntimeException`, or `Exception` in try-catch blocks

**Rationale:**
- Hides bugs and programming errors
- Makes debugging harder
- Violates fail-fast principle

**Examples:**

```java
// ❌ BAD
try {
  productService.findProduct(productId);
} catch (Exception e) {
  log.error("Error occurred", e);
}

try {
  amount.divide(divisor);
} catch (NullPointerException e) {
  return BigDecimal.ZERO;
}

// ✅ GOOD
try {
  productService.findProduct(productId);
} catch (ProductNotFoundException e) {
  log.error("Product not found: {}", productId, e);
  throw new BusinessException("Product does not exist", e);
}

try {
  return amount.divide(divisor, RoundingMode.HALF_UP);
} catch (ArithmeticException e) {
  log.error("Division error: {} / {}", amount, divisor, e);
  throw new CalculationException("Cannot divide by zero", e);
}
```

### 5. Final Method Arguments
**Rule:** All method arguments should be declared `final`

**Examples:**

```java
// ❌ BAD
public MovementDTO addMovement(UUID userId, ProductId productId, AddMovementCommand command) {
  // implementation
}

// ✅ GOOD
public MovementDTO addMovement(
    final UUID userId,
    final ProductId productId,
    final AddMovementCommand command) {
  // implementation
}
```

---

## SOLID Principles

### 1. Single Responsibility Principle (SRP)
**Definition:** A class should have only one reason to change

**Application:**
- Input Ports: Only orchestrate use case execution
- Services: Only contain business logic for one domain concept
- Repositories: Only handle data persistence for one entity
- REST Adapters: Only handle HTTP concerns for one feature

**Examples:**

```java
// ❌ BAD - Multiple responsibilities
public class ProductService {
  public void createProduct() { }
  public void sendEmail() { }
  public void generateReport() { }
  public void validateUser() { }
}

// ✅ GOOD - Single responsibility
public class ProductService {
  public void createProduct() { }
  public void updateProduct() { }
  public void deleteProduct() { }
}

public class EmailService {
  public void sendEmail() { }
}

public class ReportService {
  public void generateReport() { }
}
```

### 2. Open/Closed Principle (OCP)
**Definition:** Software entities should be open for extension but closed for modification

**Application:**
- Use interfaces (Input/Output Ports) for abstraction
- Extend behavior through composition, not modification
- Use Strategy pattern for varying algorithms

### 3. Liskov Substitution Principle (LSP)
**Definition:** Subtypes must be substitutable for their base types

**Application:**
- Input Ports must fully implement their UseCase interfaces
- Repository Adapters must honor their Output Port contracts
- No behavioral surprises when substituting implementations

### 4. Interface Segregation Principle (ISP)
**Definition:** Clients should not be forced to depend on interfaces they don't use

**Application:**
- Split large repositories into Query and Writer ports
- Keep UseCase interfaces focused on single operations
- Don't create "god interfaces"

**Examples:**

```java
// ❌ BAD - Fat interface
public interface ProductRepository {
  ProductDTO findById(ProductId id);
  List<ProductDTO> findAll();
  void save(ProductDTO product);
  void delete(ProductId id);
  List<ProductDTO> generateReport();
  void sendNotification();
}

// ✅ GOOD - Segregated interfaces
public interface ProductQueryRepository {
  ProductDTO findById(ProductId id);
  List<ProductDTO> findAll();
}

public interface ProductWriterRepository {
  void save(ProductDTO product);
  void delete(ProductId id);
}
```

### 5. Dependency Inversion Principle (DIP)
**Definition:** High-level modules should not depend on low-level modules. Both should depend on abstractions.

**Application:**
- Input Ports depend on Output Port interfaces (not implementations)
- Services depend on repository interfaces (not JPA repositories)
- Application layer NEVER depends on infrastructure

**Examples:**

```java
// ❌ BAD - Depends on concrete implementation
public class AddMovementInputPort {
  private final AccountMovementJPARepository jpaRepo; // ❌ Infra dependency!
}

// ✅ GOOD - Depends on abstraction
public class AddMovementInputPort {
  private final AccountMovementQueryRepository repository; // ✅ Port interface!
}
```

---

## Code Conventions

### 1. Boolean Parameter Anti-Pattern
**Rule:** Do NOT use boolean parameters in methods (except constructors)

**Rationale:**
- Boolean flags make method calls unclear
- Creates conditional logic branches
- Violates SRP

**Examples:**

```java
// ❌ BAD
public List<MovementDTO> findMovements(
    final UUID userId,
    final boolean includeDeleted) {
  // implementation
}
// Call site is unclear: findMovements(userId, true) - what does true mean?

// ✅ GOOD - Separate methods
public List<MovementDTO> findActiveMovements(final UUID userId) {
  // implementation
}

public List<MovementDTO> findAllMovements(final UUID userId) {
  // implementation
}
```

### 2. Maximum 3 Arguments Per Method
**Rule:** A method/function should not have more than 3 arguments

**Solution:** Introduce Value Objects to group related parameters

**Examples:**

```java
// ❌ BAD - 4 arguments
public List<MonthlyBalanceDTO> findBalancesByProductIdsAndPeriods(
    final List<ProductId> productIds,
    final YearMonth startPeriod,
    final YearMonth endPeriod,
    final boolean endPeriodExclusive) {
  // implementation
}

// ✅ GOOD - Domain Value Object
public List<MonthlyBalanceDTO> findBalancesByProductIdsAndPeriods(
    final List<ProductId> productIds,
    final PeriodRange periodRange) {
  // implementation
}

// Value Object
public record PeriodRange(YearMonth start, YearMonth end, boolean endExclusive) {
  public static PeriodRange inclusive(final YearMonth start, final YearMonth end) {
    return new PeriodRange(start, end, false);
  }

  public static PeriodRange exclusive(final YearMonth start, final YearMonth end) {
    return new PeriodRange(start, end, true);
  }
}

// Call site
findBalancesByProductIdsAndPeriods(productIds, PeriodRange.inclusive(start, end));
```

### 3. Self-Documenting Methods
**Rule:** Methods should be self-documenting through clear naming

**Guidelines:**
- Use descriptive verb + noun combinations
- Avoid abbreviations
- Name should describe what the method does, not how

**Examples:**

```java
// ❌ BAD
public void proc() { }
public void doStuff() { }
public void handle(Data d) { }

// ✅ GOOD
public void processMovement() { }
public void calculateMonthlyBalance() { }
public void validateProductOwnership() { }
```

### 4. Value Object Characteristics
**Rule:** Value Objects should provide named constructors

**Guidelines:**
- Use `of()` for simple construction
- Use descriptive factory methods for complex cases
- Make records immutable by design

**Examples:**

```java
// ✅ GOOD
public record ProductId(UUID value) {
  public static ProductId of(final UUID value) {
    return new ProductId(value);
  }

  public static ProductId generate() {
    return new ProductId(UUID.randomUUID());
  }
}

public record PeriodRange(YearMonth start, YearMonth end, boolean endExclusive) {
  public static PeriodRange inclusive(final YearMonth start, final YearMonth end) {
    return new PeriodRange(start, end, false);
  }

  public static PeriodRange exclusive(final YearMonth start, final YearMonth end) {
    return new PeriodRange(start, end, true);
  }

  public static PeriodRange singleMonth(final YearMonth month) {
    return new PeriodRange(month, month, false);
  }
}

// Usage
ProductId.of(uuid)
ProductId.generate()
PeriodRange.inclusive(start, end)
PeriodRange.singleMonth(YearMonth.now())
```

---

## Best Practices Enforcement

### 1. High Cohesion
- Each module should have a clear, focused responsibility
- Related functionality stays together
- Minimize coupling between modules

### 2. Proper Exception Handling
- Always catch specific exceptions
- Log meaningful context
- Rethrow wrapped in business exceptions when appropriate
- Never swallow exceptions silently

**Examples:**

```java
// ❌ BAD
try {
  service.execute();
} catch (Exception e) {
  // Silent failure
}

// ✅ GOOD
try {
  service.execute();
} catch (ProductNotFoundException e) {
  log.error("Product lookup failed for user {}: {}", userId, e.getMessage(), e);
  throw new BusinessException("Unable to process request: product not found", e);
}
```

### 3. Logging Best Practices
- Use SLF4J API only
- Include contextual information
- Use appropriate log levels
- Never log sensitive data

**Examples:**

```java
// ✅ GOOD
log.debug("Processing movement for product: {}", productId);
log.info("Movement added successfully: {}", movementId);
log.warn("Balance discrepancy detected for product: {}", productId);
log.error("Failed to process movement: {}", e.getMessage(), e);
```

### 4. Performance Optimization
- Avoid N+1 query problems
- Use batch operations when appropriate
- Cache expensive computations
- Use streaming for large datasets

### 5. Clean Code Practices
- Keep methods short (< 20 lines ideally)
- Avoid deep nesting (max 3 levels)
- Extract complex conditions into named methods
- Use early returns to reduce nesting

**Examples:**

```java
// ❌ BAD - Deep nesting
public void process(final ProductDTO product) {
  if (product != null) {
    if (product.isActive()) {
      if (product.hasBalance()) {
        // deep logic
      }
    }
  }
}

// ✅ GOOD - Early returns
public void process(final ProductDTO product) {
  if (product == null) {
    return;
  }

  if (!product.isActive()) {
    return;
  }

  if (!product.hasBalance()) {
    return;
  }

  // logic at top level
}
```

---

## Repository Best Practices

### Rule: Repositories Only in Services
**Principle:** Input Ports should depend on Services, NOT directly on Repositories

**Rationale:**
- Services encapsulate complex business logic
- Services can coordinate multiple repositories
- Input Ports remain focused on orchestration

**Examples:**

```java
// ❌ BAD - Input Port directly uses Repository
public class AddMovementInputPort implements AddMovementUseCase {
  private final AccountMovementQueryRepository repository; // ❌ Direct repository dependency

  @Override
  public MovementDTO addMovement(final UUID userId, final AddMovementCommand command) {
    return repository.save(movement); // ❌ Direct persistence call
  }
}

// ✅ GOOD - Input Port uses Service
public class AddMovementInputPort implements AddMovementUseCase {
  private final MovementApplicationService movementService; // ✅ Service dependency
  private final ProductsService productsService;

  @Override
  public MovementDTO addMovement(final UUID userId, final AddMovementCommand command) {
    final ProductDTO product = productsService.findByUserAndProductId(userId, productId);
    return movementService.addMovementProcessingBalances(productId, command); // ✅ Service orchestration
  }
}
```

---

## Architecture Quality Checklist

- [ ] All variables declared `final` when possible
- [ ] No literals in if statements
- [ ] No generic exception handling
- [ ] No explicit `java.lang.Exception` throws
- [ ] SOLID principles applied
- [ ] Methods have ≤ 3 parameters (use Value Objects if needed)
- [ ] No boolean parameters (except constructors)
- [ ] Repositories only in Services, NOT in Input Ports
- [ ] Self-documenting method names
- [ ] Proper exception handling with context
- [ ] High cohesion within modules
- [ ] Low coupling between modules
- [ ] Separation of concerns maintained
- [ ] Framework-agnostic domain/application layers
