# CRITICAL RULES

## Code Conventions

1. Boolean Parameter Anti-Pattern (Do not used it on any method). It's only allowed to use it in the constructors.
2. A method/function should not have more than 3 arguments. Introduce value objects to improve it. (Domain Value Objects)
   Example: This is a wrong/anti pattern function. It has 4 arguments

```java
public List<MonthlyBalanceDTO> findByProductIdsAndPeriods(
    final List<ProductId> productIds, final YearMonth startPeriod, final YearMonth endPeriod, final boolean endPeriodExclusive)
```

And can be refactored like this:

```java
// Introducing a domain value object PeriodRange

public List<MonthlyBalanceDTO> findByProductIdsAndPeriod(
    final List<ProductId> productIds,
    final PeriodRange periodRange);

// Callers
findByProductIdsAndPeriod(productIds, PeriodRange.inclusive(start, end));
```

3. The methods should be self-documenting.
4. Value Object characteristics: Provide name constructors.

## Best Practices Enforcement

- SOLID principles application
- Proper exception handling and logging
- Performance optimization recommendations
- Clean code practices
- Every module should have a highly cohesion
- Every single code block generated should use final variables and final arguments.
- Modules demonstrate proper separation of concerns
- Code follows established patterns and conventions
- Solutions are cost-effective and deployment-ready

### Architecture Recommendations

- Explain reasoning behind architectural decisions
- Provide alternative approaches when applicable
- Include trade-off analysis for different solutions
- Reference SOLID principles and clean architecture concepts
- The system is divided into self-contained modules with clear responsibilities.

### Design Patterns/Principles

Always try to apply the following principles:

- SRP (Single Responsability Principle)
- SOLID
- The repositories should only be included into service classes.
- The Input port classes should have dependency of services rather than repositories.

### Documentation

- Include clear explanations for complex implementations
- Provide setup and configuration instructions
- Document API contracts and data models
- Explain integration patterns between modules