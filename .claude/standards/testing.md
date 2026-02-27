# Testing Standards

This document defines testing requirements, coverage expectations, and best practices for the codebase.

---

## Testing Requirements

### 1. Minimum Coverage Requirement

**Rule:** Always generate unit tests for new code generated on any module different from `jbh-z-assembly` and `jbh-*****-infra`.

**Minimum Coverage:** 50%

### 2. Modules Requiring Tests

**✅ MUST have tests:**

- `jbh-*-domain` modules (domain entities, value objects)
- `jbh-*-application` modules (use cases, services, mappers, commands)

**❌ Tests NOT required:**

- `jbh-z-assembly` module (application bootstrap)
- `jbh-*-infra` modules (infrastructure adapters, REST controllers, JPA repositories)

**Rationale:**

- Domain and application layers contain core business logic
- Infrastructure is tested via integration tests
- Assembly module is configuration only

---

## Test Structure

### Unit Test Naming Convention

```
<ClassName>Test
```

**Examples:**

- `AddMovementInputPortTest`
- `MovementApplicationServiceTest`
- `ProductMapperTest`
- `AddMovementCommandTest`
- `ProductIdTest`

### Test Package Structure

Tests should mirror the source package structure:

```
# Source
src/main/java/com/jbh/finance/application/feature/movement/usecases/AddMovementInputPort.java

# Test
src/test/java/com/jbh/finance/application/feature/movement/usecases/AddMovementInputPortTest.java
```

---

## Test Fixtures

**Purpose:** Centralize test data creation to avoid polluting production code with test-only constants.

### Package Structure

**Location:** `src/test/java/com/jbh/<module>/testfixtures/`

```
jbh-finance-domain/src/test/java/
└── com/jbh/finance/testfixtures/
    ├── CategoryFixtures.java
    ├── MovementFixtures.java
    └── ProductFixtures.java
```

### Fixtures Class Pattern

**Rules:**

- Never put test-only constants in production code
- Use clear, descriptive constant names
- Group related fixtures together
- Make fixtures classes `final` with private constructor
- Use `public static final` constants for pre-built instances

**Example:**

```java
package com.jbh.finance.testfixtures;

import com.jbh.finance.domain.category.CategoryDomain;
import com.jbh.finance.domain.category.ExpenseCategory;
import com.jbh.finance.domain.category.IncomeCategory;

/**
 * Test fixtures for CategoryDomain objects.
 * Provides pre-configured category instances for testing purposes.
 */
public final class CategoryFixtures {

  public static final CategoryDomain OTHER_INCOME =
      CategoryDomain.withCategoryType(IncomeCategory.OTHER);

  public static final CategoryDomain PERSONAL_EXPENSE =
      CategoryDomain.withCategoryType(ExpenseCategory.PERSONAL);

  public static final CategoryDomain SALARY_INCOME =
      CategoryDomain.withCategoryType(IncomeCategory.SALARY);

  private CategoryFixtures() {
    throw new AssertionError("Utility class - do not instantiate");
  }
}
```

### Using Fixtures in Tests

Import fixtures using static imports:

```java
import static com.jbh.finance.testfixtures.CategoryFixtures.OTHER_INCOME;
import static com.jbh.finance.testfixtures.CategoryFixtures.PERSONAL_EXPENSE;

@Test
void shouldCreateMovementWithCategory() {
  final MovementDomain movement = MovementDomain.builder()
      .category(OTHER_INCOME)
      .amount(new BigDecimal("100.00"))
      .build();

  assertEquals(OTHER_INCOME, movement.getCategory());
}
```

### Benefits

1. **Clean Production Code** - No test-only constants in domain/application layers
2. **Easy Discovery** - All test data in one place
3. **Reduced Noise** - Searching for production usage doesn't show test references
4. **Reusability** - All tests import from same location
5. **Maintainability** - Change test data in one place

### When to Use Fixtures

✅ **Use fixtures for:**

- Common domain objects used across many tests
- Predefined categories, types, or enums
- Standard test scenarios (valid/invalid states)
- Frequently used value objects

❌ **Don't use fixtures for:**

- Test-specific data that varies per test
- Data that needs customization per test (use builders instead)
- One-off test scenarios

---

## Test Categories

### 1. Value Object Tests

Test immutability, factory methods, and validation.

**Example:**

```java
package com.jbh.finance.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductIdTest {

  @Test
  void shouldCreateProductIdFromUUID() {
    final UUID uuid = UUID.randomUUID();
    final ProductId productId = ProductId.of(uuid);

    assertNotNull(productId);
    assertEquals(uuid, productId.value());
  }

  @Test
  void shouldGenerateNewProductId() {
    final ProductId productId = ProductId.generate();

    assertNotNull(productId);
    assertNotNull(productId.value());
  }

  @Test
  void shouldThrowExceptionForNullValue() {
    assertThrows(IllegalArgumentException.class, () -> ProductId.of(null));
  }

  @Test
  void shouldBeEqualWhenUUIDsMatch() {
    final UUID uuid = UUID.randomUUID();
    final ProductId id1 = ProductId.of(uuid);
    final ProductId id2 = ProductId.of(uuid);

    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());
  }
}
```

### 2. Command Object Tests

Test validation logic and builder patterns.

**Example:**

```java
package com.jbh.finance.application.feature.movement.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddMovementCommandTest {

  @Test
  void shouldCreateValidCommand() {
    final AddMovementCommand command = AddMovementCommand.builder()
        .entryDate(LocalDate.now())
        .totalAmount(BigDecimal.valueOf(100))
        .balanceSnapshot(BigDecimal.valueOf(1000))
        .movementType(MovementType.DEPOSIT)
        .categoryDTO(MovementCategoryVO.of("Salary"))
        .description("Monthly salary")
        .build();

    assertDoesNotThrow(command::validate);
  }

  @Test
  void shouldFailValidationWhenEntryDateIsNull() {
    final AddMovementCommand command = AddMovementCommand.builder()
        .entryDate(null)
        .totalAmount(BigDecimal.valueOf(100))
        .balanceSnapshot(BigDecimal.valueOf(1000))
        .movementType(MovementType.DEPOSIT)
        .categoryDTO(MovementCategoryVO.of("Salary"))
        .description("Monthly salary")
        .build();

    assertThrows(IllegalArgumentException.class, command::validate);
  }

  @Test
  void shouldFailValidationWhenAmountIsNegative() {
    final AddMovementCommand command = AddMovementCommand.builder()
        .entryDate(LocalDate.now())
        .totalAmount(BigDecimal.valueOf(-100))
        .balanceSnapshot(BigDecimal.valueOf(1000))
        .movementType(MovementType.DEPOSIT)
        .categoryDTO(MovementCategoryVO.of("Salary"))
        .description("Monthly salary")
        .build();

    assertThrows(IllegalArgumentException.class, command::validate);
  }

  @Test
  void shouldFailValidationWhenDescriptionIsBlank() {
    final AddMovementCommand command = AddMovementCommand.builder()
        .entryDate(LocalDate.now())
        .totalAmount(BigDecimal.valueOf(100))
        .balanceSnapshot(BigDecimal.valueOf(1000))
        .movementType(MovementType.DEPOSIT)
        .categoryDTO(MovementCategoryVO.of("Salary"))
        .description("")
        .build();

    assertThrows(IllegalArgumentException.class, command::validate);
  }
}
```

### 3. Input Port Tests

Test use case orchestration, validation, and error handling.

**Example:**

```java
package com.jbh.finance.application.feature.movement.ports.input;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddMovementInputPortTest {

  @Mock
  private MovementApplicationService movementService;

  @Mock
  private ProductsService productsService;

  private AddMovementInputPort inputPort;

  @BeforeEach
  void setUp() {
    inputPort = new AddMovementInputPort(movementService, productsService);
  }

  @Test
  void shouldAddMovementSuccessfully() {
    // Given
    final UUID userId = UUID.randomUUID();
    final ProductId productId = ProductId.generate();
    final AddMovementCommand command = createValidCommand();

    final ProductDTO product = createProductDTO(userId, productId);
    final AddBasicMovementDTO expectedResult = createMovementDTO();

    when(productsService.findByUserAndProductId(userId, productId)).thenReturn(product);
    when(movementService.addMovementProcessingBalances(any(), eq(command))).thenReturn(expectedResult);

    // When
    final AddBasicMovementDTO result = inputPort.addMovement(userId, productId, command);

    // Then
    assertNotNull(result);
    assertEquals(expectedResult, result);
    verify(productsService).findByUserAndProductId(userId, productId);
    verify(movementService).addMovementProcessingBalances(any(), eq(command));
  }

  @Test
  void shouldThrowExceptionWhenUserIdIsNull() {
    // Given
    final ProductId productId = ProductId.generate();
    final AddMovementCommand command = createValidCommand();

    // When & Then
    assertThrows(GenericSpecificationException.class,
        () -> inputPort.addMovement(null, productId, command));

    verifyNoInteractions(productsService);
    verifyNoInteractions(movementService);
  }

  @Test
  void shouldThrowExceptionWhenProductIdIsNull() {
    // Given
    final UUID userId = UUID.randomUUID();
    final AddMovementCommand command = createValidCommand();

    // When & Then
    assertThrows(GenericSpecificationException.class,
        () -> inputPort.addMovement(userId, null, command));

    verifyNoInteractions(productsService);
    verifyNoInteractions(movementService);
  }

  @Test
  void shouldThrowExceptionWhenCommandIsNull() {
    // Given
    final UUID userId = UUID.randomUUID();
    final ProductId productId = ProductId.generate();

    // When & Then
    assertThrows(GenericSpecificationException.class,
        () -> inputPort.addMovement(userId, productId, null));

    verifyNoInteractions(productsService);
    verifyNoInteractions(movementService);
  }

  @Test
  void shouldThrowExceptionWhenUserDoesNotOwnProduct() {
    // Given
    final UUID userId = UUID.randomUUID();
    final ProductId productId = ProductId.generate();
    final AddMovementCommand command = createValidCommand();

    when(productsService.findByUserAndProductId(userId, productId))
        .thenThrow(new BusinessException("Product not found"));

    // When & Then
    assertThrows(BusinessException.class,
        () -> inputPort.addMovement(userId, productId, command));

    verify(productsService).findByUserAndProductId(userId, productId);
    verifyNoInteractions(movementService);
  }

  private AddMovementCommand createValidCommand() {
    return AddMovementCommand.builder()
        .entryDate(LocalDate.now())
        .totalAmount(BigDecimal.valueOf(100))
        .balanceSnapshot(BigDecimal.valueOf(1000))
        .movementType(MovementType.DEPOSIT)
        .categoryDTO(MovementCategoryVO.of("Salary"))
        .description("Test movement")
        .build();
  }

  private ProductDTO createProductDTO(final UUID userId, final ProductId productId) {
    return ProductDTO.builder()
        .id(productId)
        .userId(userId)
        .name("Test Product")
        .balance(BigDecimal.valueOf(1000))
        .build();
  }

  private AddBasicMovementDTO createMovementDTO() {
    return AddBasicMovementDTO.builder()
        .id(MovementId.generate())
        .balance(BigDecimal.valueOf(1100))
        .build();
  }
}
```

### 4. Service Tests

Test business logic, multi-step operations, and error scenarios.

**Example:**

```java
package com.jbh.finance.application.feature.movement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovementApplicationServiceTest {

  @Mock
  private AccountMovementQueryRepository repository;

  @Mock
  private ProductsService productsService;

  private MovementApplicationService service;

  @BeforeEach
  void setUp() {
    service = new MovementApplicationServiceImpl(repository, productsService);
  }

  @Test
  void shouldAddMovementAndUpdateBalance() {
    // Given
    final ProductPK productPK = new ProductPK(UUID.randomUUID(), ProductId.generate());
    final AddMovementCommand command = createValidCommand();
    final ProductDTO product = createProductDTO(productPK);

    when(productsService.findByPK(productPK)).thenReturn(product);
    when(repository.save(any())).thenReturn(createMovementDTO());

    // When
    final AddBasicMovementDTO result = service.addMovementProcessingBalances(productPK, command);

    // Then
    assertNotNull(result);
    verify(productsService).findByPK(productPK);
    verify(repository).save(any());
    verify(productsService).updateBalance(eq(productPK), any());
  }

  @Test
  void shouldCalculateNewBalanceCorrectly() {
    // Given
    final BigDecimal currentBalance = BigDecimal.valueOf(1000);
    final BigDecimal depositAmount = BigDecimal.valueOf(500);

    // When
    final BigDecimal newBalance = service.calculateBalance(
        currentBalance,
        depositAmount,
        MovementType.DEPOSIT);

    // Then
    assertEquals(BigDecimal.valueOf(1500), newBalance);
  }

  @Test
  void shouldDecrementBalanceForWithdrawal() {
    // Given
    final BigDecimal currentBalance = BigDecimal.valueOf(1000);
    final BigDecimal withdrawalAmount = BigDecimal.valueOf(300);

    // When
    final BigDecimal newBalance = service.calculateBalance(
        currentBalance,
        withdrawalAmount,
        MovementType.WITHDRAWAL);

    // Then
    assertEquals(BigDecimal.valueOf(700), newBalance);
  }

  private AddMovementCommand createValidCommand() {
    return AddMovementCommand.builder()
        .entryDate(LocalDate.now())
        .totalAmount(BigDecimal.valueOf(500))
        .balanceSnapshot(BigDecimal.valueOf(1000))
        .movementType(MovementType.DEPOSIT)
        .categoryDTO(MovementCategoryVO.of("Salary"))
        .description("Test movement")
        .build();
  }

  private ProductDTO createProductDTO(final ProductPK productPK) {
    return ProductDTO.builder()
        .id(productPK.productId())
        .userId(productPK.userId())
        .name("Test Product")
        .balance(BigDecimal.valueOf(1000))
        .build();
  }

  private MovementDTO createMovementDTO() {
    return MovementDTO.builder()
        .id(MovementId.generate())
        .movementAmount(BigDecimal.valueOf(500))
        .balanceSnapshot(BigDecimal.valueOf(1500))
        .build();
  }
}
```

### 5. Mapper Tests

Test DTO to entity mappings and vice versa.

**Example:**

```java
package com.jbh.finance.application.feature.product.mappers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

  @Test
  void shouldMapDomainEntityToDTO() {
    // Given
    final Product product = createDomainProduct();

    // When
    final ProductDTO dto = ProductMapper.toDTO(product);

    // Then
    assertNotNull(dto);
    assertEquals(product.getId(), dto.id());
    assertEquals(product.getName(), dto.name());
    assertEquals(product.getBalance(), dto.balance());
    assertEquals(product.getUserId(), dto.userId());
  }

  @Test
  void shouldMapDTOToDomainEntity() {
    // Given
    final ProductDTO dto = createProductDTO();

    // When
    final Product product = ProductMapper.toDomain(dto);

    // Then
    assertNotNull(product);
    assertEquals(dto.id(), product.getId());
    assertEquals(dto.name(), product.getName());
    assertEquals(dto.balance(), product.getBalance());
    assertEquals(dto.userId(), product.getUserId());
  }

  @Test
  void shouldHandleNullValuesGracefully() {
    // Given
    final Product product = createProductWithNullFields();

    // When
    final ProductDTO dto = ProductMapper.toDTO(product);

    // Then
    assertNotNull(dto);
    assertNull(dto.description());
    assertNull(dto.metadata());
  }

  private Product createDomainProduct() {
    return Product.builder()
        .id(ProductId.generate())
        .userId(UUID.randomUUID())
        .name("Test Product")
        .balance(BigDecimal.valueOf(1000))
        .build();
  }

  private ProductDTO createProductDTO() {
    return ProductDTO.builder()
        .id(ProductId.generate())
        .userId(UUID.randomUUID())
        .name("Test Product")
        .balance(BigDecimal.valueOf(1000))
        .build();
  }

  private Product createProductWithNullFields() {
    return Product.builder()
        .id(ProductId.generate())
        .userId(UUID.randomUUID())
        .name("Test Product")
        .balance(BigDecimal.valueOf(1000))
        .description(null)
        .metadata(null)
        .build();
  }
}
```

---

## Testing Best Practices

### 1. Test Method Naming

Use descriptive names that explain what is being tested:

**Pattern:**

```
should<ExpectedBehavior>When<Condition>
```

**Examples:**

- `shouldCreateProductSuccessfully()`
- `shouldThrowExceptionWhenUserIdIsNull()`
- `shouldReturnEmptyListWhenNoMovementsExist()`
- `shouldCalculateBalanceCorrectlyForDeposit()`

### 2. AAA Pattern (Arrange-Act-Assert)

Structure tests in three clear sections:

```java

@Test
void shouldAddMovementSuccessfully() {
  // Arrange (Given)
  final UUID userId = UUID.randomUUID();
  final AddMovementCommand command = createValidCommand();
  when(service.execute(command)).thenReturn(expectedResult);

  // Act (When)
  final MovementDTO result = inputPort.addMovement(userId, command);

  // Assert (Then)
  assertNotNull(result);
  assertEquals(expectedResult, result);
}
```

### 3. Use Mockito for Dependencies

Mock all external dependencies (services, repositories):

```java

@ExtendWith(MockitoExtension.class)
class MyTest {

  @Mock
  private DependencyService service;

  @Mock
  private DependencyRepository repository;

  @BeforeEach
  void setUp() {
    // Initialize class under test
    myClass = new MyClass(service, repository);
  }
}
```

### 4. Test Edge Cases

Always test:

- Null inputs
- Empty collections
- Boundary values
- Invalid states
- Error scenarios

### 5. Avoid Test Dependencies

Each test should be independent and runnable in isolation:

```java
// ❌ BAD - Tests depend on execution order
private static Product product;

@Test
void test1_createProduct() {
  product = service.create();
}

@Test
void test2_updateProduct() {
  service.update(product); // Depends on test1
}

// ✅ GOOD - Independent tests
@Test
void shouldCreateProduct() {
  final Product product = service.create();
  assertNotNull(product);
}

@Test
void shouldUpdateProduct() {
  final Product product = createTestProduct(); // Self-contained
  service.update(product);
}
```

### 6. Use Test Builders/Factories

Create helper methods for test data:

```java
private AddMovementCommand createValidCommand() {
  return AddMovementCommand.builder()
      .entryDate(LocalDate.now())
      .totalAmount(BigDecimal.valueOf(100))
      .movementType(MovementType.DEPOSIT)
      .description("Test")
      .build();
}

private ProductDTO createProductDTO() {
  return ProductDTO.builder()
      .id(ProductId.generate())
      .name("Test Product")
      .balance(BigDecimal.ZERO)
      .build();
}
```

---

## Coverage Enforcement

### JaCoCo Configuration

Add JaCoCo plugin to module POMs (exclude infra and assembly):

```xml

<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.10</version>
  <executions>
    <execution>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
    <execution>
      <id>check</id>
      <goals>
        <goal>check</goal>
      </goals>
      <configuration>
        <rules>
          <rule>
            <element>PACKAGE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.50</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Coverage Report

Run coverage report:

```bash
mvn clean test jacoco:report
```

View report:

```
target/site/jacoco/index.html
```

---

## Test Checklist

When writing tests for new code, verify:

- [ ] Minimum 50% line coverage achieved
- [ ] All public methods tested
- [ ] Null parameter scenarios tested
- [ ] Invalid input scenarios tested
- [ ] Business rule violations tested
- [ ] Happy path scenarios tested
- [ ] Error handling tested
- [ ] Edge cases covered
- [ ] Tests are independent (no execution order dependencies)
- [ ] Test names are descriptive
- [ ] AAA pattern used (Arrange-Act-Assert)
- [ ] Mocks used for dependencies
- [ ] Test data builders/factories created
- [ ] No tests written for infra or assembly modules
