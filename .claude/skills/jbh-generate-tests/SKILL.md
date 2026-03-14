---
name: jbh-generate-tests
description: Interactively generates unit tests for domain/application classes following project test standards
user-invocable: true
---

# Generate Unit Tests Skill

You are helping the user generate a correct, standards-compliant unit test class for a class in the `*-domain` or `*-application` modules. Follow every instruction below exactly and in order.

---

## Step 1 — Gather Information

Ask the user the following questions (you may ask all at once):

1. **What class do you want to test?**
   - Provide the full class name (e.g., `AddMovementInputPort`, `ProductMapperImpl`, `ProductId`, `AddMovementCommand`, `MovementApplicationServiceImpl`)
   - What type is it? (input port / use case, service, mapper, value object, or command)
2. **What feature does it belong to?**
   - Feature name (e.g., `movement`, `product`, `monthlybalance`, `category`)
3. **Which module?**
   - `*-domain` or `*-application`

Wait for the user's answers before proceeding.

---

## Step 2 — Read the Class Under Test

Before generating any test code, read the actual source file to understand:
- Constructor parameters and dependencies
- Public methods and their signatures
- Validation logic and business rules
- Return types and thrown exceptions

Search for the file using Glob in the appropriate module's `src/main/java` directory.

---

## Step 3 — Decide Test Strategy

Based on class type, select the correct strategy:

| Class Type | Tag | Dependencies |
|---|---|---|
| Input port / use case with multi-step flows (uses output ports / repositories) | `@Tag("integration")` | `InMemory*` fakes from `testfixtures/fakes/` |
| Input port / service (purely delegates, no state) | `@Tag("unit")` | Mockito `@Mock` + `@ExtendWith(MockitoExtension.class)` |
| Mapper | `@Tag("unit")` | No mocks needed — instantiate directly |
| Command / Value Object | `@Tag("unit")` | No mocks needed — instantiate directly |

**Rule:** When `InMemory*` fakes already exist for the feature in `testfixtures/fakes/<feature>/`, prefer using them (integration style). When they don't exist, use Mockito (unit style).

Check `testfixtures/fakes/` to see what fakes are already available before deciding.

---

## Step 4 — Determine Test Package and File Location

**Test package pattern:**

```
com.jbh.finance.test.application.feature.<feature>/<subpackage>/
```

Subpackage mapping by class type:
- Input port → `ports/input/`
- Use case → `usecases/`
- Service → `services/`
- Mapper → `mappers/` (or omit if none exists — check existing structure)
- Command → `core/vo/commands/` (cross-cutting) OR `feature/<feature>/commands/` if feature-specific
- Value Object → match the domain package structure under `feature/<feature>/`

**File location:**

```
jbh-finance-application/src/test/java/com/jbh/finance/test/application/feature/<feature>/<subpackage>/<ClassName>Test.java
```

**Naming rule:** Always `<ClassName>Test` — no `IT`, `Mock`, or other suffixes.

---

## Step 5 — Generate the Test Class

Generate a complete, compilable test class following all rules below:

### Required Structure

```java
package com.jbh.finance.test.application.feature.<feature>.<subpackage>;

// imports...

@Tag("<unit|integration>")
// @ExtendWith(MockitoExtension.class)  ← only for unit/Mockito tests
class <ClassName>Test {

  // @Mock fields  ← only for Mockito tests
  // InMemory* fields  ← only for integration tests

  private <ClassUnderTest> <instanceVar>;

  @BeforeEach
  void setUp() {
    // initialize class under test with mocks or fakes
  }

  // --- Happy path ---

  @Test
  void should<ExpectedBehavior>When<HappyCondition>() {
    // Given
    ...
    // When
    ...
    // Then
    ...
  }

  // --- Null parameter tests (one per required param) ---

  @Test
  void shouldThrowExceptionWhen<ParameterName>IsNull() {
    // Given
    ...
    // When & Then
    assertThrows(..., () -> ...);
  }

  // --- Business rule violation tests ---

  @Test
  void shouldThrowExceptionWhen<BusinessRule>IsViolated() {
    // Given
    ...
    // When & Then
    assertThrows(..., () -> ...);
  }

  // --- Private factory methods ---

  private <CommandOrDTO> create<Name>() {
    return ...builder()...build();
  }
}
```

### Mandatory Rules

- All local variables declared `final`
- No literals in assertions — use named variables
- Test method names: `shouldXxxWhenYyy()` pattern
- AAA pattern with `// Given`, `// When`, `// Then` comments (or `// When & Then` for combined)
- One assertion concept per test
- Private helper factory methods for all test data objects (no inline object construction in test body)
- Minimum tests to generate:
  - 1 happy path test
  - 1 null test per required constructor/method parameter
  - 1+ business rule violation tests (inferred from validation logic)
- Use `assertThrows` for exception tests, never `try/catch`
- For Mockito tests: use `verify()` to confirm interactions; use `verifyNoInteractions()` when service should NOT be called

---

## Step 6 — Ask About Fixture Class

After presenting the test class, ask:

> **Do you want me to also generate a fixture class in `testfixtures/` for shared test data?**
>
> This is useful if the same test data (e.g., commands, DTOs, domain objects) will be reused across multiple test classes for the `<feature>` feature.

If the user says **yes**:

Generate a fixture class in the appropriate location:

```
jbh-finance-application/src/test/java/com/jbh/finance/test/testfixtures/builders/commands/<Feature>CommandFixture.java
```

or for domain fixtures:

```
jbh-finance-domain/src/test/java/com/jbh/finance/testfixtures/<Feature>Fixtures.java
```

Fixture class rules:
- `public final` class with `private` constructor throwing `AssertionError`
- `public static final` constants for pre-built instances
- Use `public static` factory methods for parameterized variants
- Class name uses `*Fixtures` or `*Fixture` suffix — never `*Test`

Example:
```java
public final class MovementFixtures {

  public static final AddMovementCommand VALID_DEPOSIT_COMMAND =
      AddMovementCommand.builder()
          .entryDate(LocalDate.of(2026, 1, 15))
          .totalAmount(BigDecimal.valueOf(500))
          .movementType(MovementType.DEPOSIT)
          .description("Salary deposit")
          .build();

  private MovementFixtures() {
    throw new AssertionError("Utility class - do not instantiate");
  }
}
```

---

## Step 7 — Write the Files

Once the user approves the generated test (and optional fixture), write the files to disk using the Write tool. Confirm the exact file paths before writing.

---

## Reference: Testing Standards Summary

- **Modules requiring tests:** `*-domain`, `*-application` only
- **Minimum coverage:** 50%
- **No tests in:** `jbh-z-assembly`, `*-infra`
- **Fakes** → multi-step integration flows; **Mocks** → interaction verification only
- **`testfixtures/fakes/<feature>/`** — in-memory output port implementations
- **`testfixtures/builders/`** — command/entity builders
- **`testfixtures/utils/`** — shared enums and helpers (must NOT end in `Test`)
- **Never** end fixture/helper classes in `Test`
- **Never** use `@SpringBootTest` or any Quarkus injection in domain/application tests
