# Category Domain Refactoring Plan

**Date**: 2026-02-14
**Module**: jbh-finance-domain
**Status**: Planning
**Priority**: High

---

## Executive Summary

The current `CategoryDomain` implementation has **naming violations**, **missing DTOs**, **zero test coverage**, and architectural inconsistencies. This plan addresses immediate
refactoring needs and provides analysis on whether categories should remain as enums or evolve into database entities.

---

## 1. Current State Analysis

### 1.1 Existing Structure

```
jbh-finance-domain/
└── src/main/java/com/jbh/finance/domain/movement/
    ├── CategoryDomain.java              ❌ NAMING VIOLATION
    └── vo/
        ├── CategoryType.java            ✅ Interface
        ├── CategorySource.java          ✅ Enum (INCOME, EXPENSE)
        ├── IncomeCategory.java          ✅ Enum (10 categories)
        ├── ExpenseCategory.java         ✅ Enum (6 categories)
        └── MovementCategoryVO.java      ❌ INHERITANCE ISSUE
```

### 1.2 Current Issues

| Issue                                                              | Severity  | Impact                                |
|--------------------------------------------------------------------|-----------|---------------------------------------|
| Naming violation: `CategoryDomain` should be `CategoryVO`          | 🔴 High   | Violates naming standards             |
| Missing `CategoryDTO` in application module                        | 🔴 High   | Violates hexagonal architecture       |
| Zero test coverage                                                 | 🔴 High   | Violates 50% coverage requirement     |
| Confusing inheritance: `MovementCategoryVO extends CategoryDomain` | 🟡 Medium | Violates composition over inheritance |
| Static constants in domain class                                   | 🟡 Medium | Poor separation of concerns           |
| TODO comments unresolved                                           | 🟢 Low    | Technical debt                        |
| Unprofessional "CHATGPT" comment                                   | 🟢 Low    | Code quality                          |

### 1.3 Current Usage

**Used by**:

- `MovementDomain` (main domain entity)
- Movement validation logic
- Movement type determination
- File import functionality

**Dependencies**:

- Zero external dependencies (domain-pure)
- Uses `CategoryType` interface for polymorphism
- Translation support via `JbhStringUtils`

---

## 2. Naming & Architectural Corrections

### 2.1 Rename Plan

| Current Name         | Correct Name                         | Reason                                   |
|----------------------|--------------------------------------|------------------------------------------|
| `CategoryDomain`     | `CategoryVO`                         | It's a value object, not a domain entity |
| `MovementCategoryVO` | ~~Delete~~ (merge into `CategoryVO`) | Unnecessary inheritance                  |

### 2.2 Add Missing DTO

Create `CategoryDTO` in **jbh-finance-application** module:

```java
package com.jbh.finance.application.feature.movement.dto;

public record CategoryDTO(
    CategorySource source,      // INCOME or EXPENSE
    String typeName,            // "SALARY", "PERSONAL", etc.
    String translationsKey      // i18n key
) {

  public static CategoryDTO from(CategoryVO vo) {
    return new CategoryDTO(
        vo.getSource(),
        vo.getType().getTypeName(),
        vo.getType().getTranslationsKey()
    );
  }
}
```

### 2.3 Simplified CategoryVO

```java
package com.jbh.finance.domain.movement.vo;

/**
 * Value Object representing a movement category.
 * Wraps CategoryType (IncomeCategory or ExpenseCategory enums).
 */
public final class CategoryVO {

  private final CategoryType categoryType;

  private CategoryVO(final CategoryType categoryType) {
    this.categoryType = categoryType;
  }

  public static CategoryVO of(final CategoryType categoryType) {
    return new CategoryVO(categoryType);
  }

  public boolean isExpense() {
    return getSource() == CategorySource.EXPENSE;
  }

  public boolean isIncome() {
    return getSource() == CategorySource.INCOME;
  }

  public CategorySource getSource() {
    return categoryType.getSource();
  }

  public CategoryType getType() {
    return categoryType;
  }
}
```

---

## 3. Testing Requirements

### 3.1 Create Unit Tests

**File**: `CategoryVOTest.java`

**Coverage Requirements**:

- Minimum 50% coverage (per project standards)
- Target: 80%+ coverage

**Test Cases**:

```java
class CategoryVOTest {

  @Nested
  class FactoryMethods {

    @Test
    void shouldCreateFromIncomeCategory() {
    }

    @Test
    void shouldCreateFromExpenseCategory() {
    }

    @Test
    void shouldThrowExceptionWhenCategoryTypeIsNull() {
    }
  }

  @Nested
  class BehaviorMethods {

    @Test
    void shouldIdentifyIncomeCategory() {
    }

    @Test
    void shouldIdentifyExpenseCategory() {
    }

    @Test
    void shouldReturnCorrectSource() {
    }

    @Test
    void shouldReturnCorrectType() {
    }
  }

  @Nested
  class EdgeCases {

    @Test
    void shouldHandleAllIncomeCategories() {
    }

    @Test
    void shouldHandleAllExpenseCategories() {
    }
  }
}
```

### 3.2 Enum Tests

**Files**: `IncomeCategoryTest.java`, `ExpenseCategoryTest.java`

**Test Cases**:

- All enum values have translations
- `findByName()` works for all values
- `getSource()` returns correct CategorySource
- Translation keys are non-empty

---

## 4. Enum vs Entity Analysis

### 4.1 Current Enum Approach

**Advantages** ✅:

1. **Type Safety**: Compile-time validation
2. **Performance**: No database lookups
3. **Simplicity**: Easy to use, no ORM overhead
4. **Version Control**: Changes tracked in Git
5. **Zero Latency**: No network calls
6. **Immutable**: Cannot be modified at runtime

**Disadvantages** ❌:

1. **Requires Code Deployment**: New categories need redeployment
2. **Not User-Configurable**: Users cannot add custom categories
3. **Limited Flexibility**: Cannot disable/enable categories dynamically
4. **No Audit Trail**: Cannot track when categories were added/removed
5. **Translation Hardcoded**: Cannot update translations without deployment

### 4.2 Entity/Table Approach

**Schema Design** (if implemented):

```sql
CREATE TABLE finance.movement_category
(
    category_id     VARCHAR(50) PRIMARY KEY, -- "SALARY", "PERSONAL"
    category_source VARCHAR(20) NOT NULL,    -- "INCOME", "EXPENSE"
    is_active       BOOLEAN DEFAULT TRUE,
    display_name    jsonb                    -- HANDLE EN AND ES LANGUAGES
        CONSTRAINT chk_source CHECK (category_source IN ('INCOME', 'EXPENSE'))
);

```

**Advantages** ✅:

1. **Dynamic Configuration**: Add/remove categories without deployment
2. **User Customization**: Users could add custom categories (future feature)
3. **Soft Delete**: Disable instead of delete (data integrity)
4. **Audit Trail**: Track category lifecycle
5. **Flexible Translations**: Update translations via admin panel
6. **Multi-tenancy Ready**: Different categories per team/user

**Disadvantages** ❌:

1. **Complexity**: ORM mapping, repositories, migrations
2. **Performance Overhead**: Database queries for each category lookup
3. **Caching Required**: Need cache layer to avoid N+1 queries
4. **Data Migration**: Convert existing enum references to IDs
5. **Validation Complexity**: Runtime validation instead of compile-time
6. **Referential Integrity**: Must ensure existing movements not broken

### 4.3 Hybrid Approach (Recommended for Future)

**Strategy**: Keep enums for now, prepare for migration

```java
public enum IncomeCategory implements CategoryType {
  SALARY("salary", "Salary", "Salario"),  // lowercase = future DB key
  // ...

  private final String databaseKey;  // For future migration

  public String getDatabaseKey() {
    return databaseKey;
  }
  }
```

**Benefits**:

- Current simplicity maintained
- Future migration easier
- Backward compatibility preserved

### 4.4 Decision Matrix

| Criteria          | Weight   | Enum Score | Entity Score | Winner   |
|-------------------|----------|------------|--------------|----------|
| Performance       | 30%      | 10/10      | 6/10         | Enum     |
| Flexibility       | 25%      | 4/10       | 10/10        | Entity   |
| Simplicity        | 20%      | 10/10      | 5/10         | Enum     |
| User Control      | 15%      | 2/10       | 10/10        | Entity   |
| Development Speed | 10%      | 10/10      | 4/10         | Enum     |
| **Total**         | **100%** | **7.5/10** | **6.95/10**  | **Enum** |

### 4.5 Recommendation: KEEP AS ENUM (For Now)

**Rationale**:

1. **Current Requirements**: No user-defined categories needed
2. **Performance Critical**: Finance calculations are frequent
3. **Simple Domain**: 16 total categories (10 income + 6 expense)
4. **Budget Constraints**: Minimize infrastructure complexity
5. **YAGNI Principle**: Don't build what you don't need yet

**Future Migration Trigger**:
Migrate to entity/table when ANY of these occur:

- Users request custom categories
- Categories exceed 50 total
- Multi-language support beyond EN/ES needed
- Category-specific business rules emerge
- Multi-tenancy requirements (different categories per team)

---

## 5. Refactoring Implementation Plan

### Phase 1: Preparation (No Breaking Changes)

**Tasks**:

1. ✅ Create unit tests for current implementation
    - `CategoryDomainTest.java`
    - `IncomeCategoryTest.java`
    - `ExpenseCategoryTest.java`
    - **Acceptance**: 80%+ coverage

2. ✅ Document all usages of `CategoryDomain`
    - Search across all modules
    - Create migration checklist
    - **Acceptance**: All usages documented

3. ✅ Add `databaseKey` field to enums (future-proofing)
    - Modify `IncomeCategory`
    - Modify `ExpenseCategory`
    - **Acceptance**: All enums have lowercase keys

**Estimated Effort**: 4 hours
**Risk**: Low

---

### Phase 2: Domain Module Refactoring

**Tasks**:

1. ✅ Rename `CategoryDomain` → `CategoryVO`
    - Update class name
    - Update all imports
    - Update tests
    - **Acceptance**: All tests pass

2. ✅ Delete `MovementCategoryVO`
    - Move factory methods to `CategoryFactory`
    - Update `MovementType` references
    - **Acceptance**: Zero compilation errors

3. ✅ Create `CategoryFactory`
    - Move static constants
    - Move complex factory methods
    - **Acceptance**: All factory methods tested

4. ✅ Clean up documentation
    - Remove "CHATGPT" comment
    - Add proper JavaDoc
    - Resolve TODOs
    - **Acceptance**: No TODO/FIXME comments remain

**Estimated Effort**: 6 hours
**Risk**: Medium (breaking changes)

---

### Phase 3: Application Module Integration

**Tasks**:

1. ✅ Create `CategoryDTO` in application module
    - Add record class
    - Add factory methods
    - **Acceptance**: Compiles successfully

2. ✅ Update application layer to use DTOs
    - Modify input ports
    - Modify use cases
    - Update mappers
    - **Acceptance**: All tests pass

3. ✅ Update infrastructure layer
    - Modify REST adapters
    - Update persistence adapters
    - **Acceptance**: Integration tests pass

**Estimated Effort**: 8 hours
**Risk**: Medium

---

### Phase 4: Testing & Validation

**Tasks**:

1. ✅ Run full test suite
    - Unit tests
    - Integration tests
    - Architecture tests (ArchUnit)
    - **Acceptance**: All tests pass

2. ✅ Code coverage verification
    - Generate coverage report
    - Verify 50%+ coverage
    - **Acceptance**: Coverage meets standards

3. ✅ Manual testing
    - Test movement creation
    - Test category validation
    - Test balance calculations
    - **Acceptance**: No regression bugs

**Estimated Effort**: 4 hours
**Risk**: Low

---

### Phase 5: Documentation & Cleanup

**Tasks**:

1. ✅ Update architecture documentation
    - Update feature structure docs
    - Update mapping flow diagrams
    - **Acceptance**: Docs reflect new structure

2. ✅ Update CLAUDE.md if needed
    - Add CategoryVO naming example
    - Update DTO guidelines
    - **Acceptance**: Claude instructions updated

3. ✅ Create migration guide (if needed for other modules)
    - Document breaking changes
    - Provide code examples
    - **Acceptance**: Other modules can migrate

**Estimated Effort**: 2 hours
**Risk**: Low

---

## 6. Migration Checklist

### Pre-Migration

- [ ] Backup current codebase
- [ ] Create feature branch `refactor/category-domain-to-vo`
- [ ] Notify team of upcoming changes

### Domain Module Changes

- [ ] Create `CategoryVOTest.java` (80%+ coverage)
- [ ] Create `IncomeCategoryTest.java`
- [ ] Create `ExpenseCategoryTest.java`
- [ ] Add `databaseKey` field to enums
- [ ] Rename `CategoryDomain.java` → `CategoryVO.java`
- [ ] Create `CategoryFactory.java`
- [ ] Delete `MovementCategoryVO.java`
- [ ] Move factory methods to `CategoryFactory`
- [ ] Update `MovementDomain` imports
- [ ] Update `MovementType` references
- [ ] Remove "CHATGPT" comment from `IncomeCategory`
- [ ] Add JavaDoc to all category classes
- [ ] Resolve TODO in deleted `MovementCategoryVO`

### Application Module Changes

- [ ] Create `CategoryDTO.java` record
- [ ] Add factory methods to `CategoryDTO`
- [ ] Update `FindMonthlyBalanceInputPort` (if uses Category)
- [ ] Update use cases (if use Category)
- [ ] Update mappers (Entity ↔ DTO)

### Infrastructure Module Changes

- [ ] Update `MonthlyBalanceRestAdapter` (if uses Category)
- [ ] Update `MovementRestAdapter` (if uses Category)
- [ ] Update persistence entities (if needed)
- [ ] Update request/response objects

### Testing

- [ ] Run `mvn clean test` (all modules)
- [ ] Run `mvn verify` (integration tests)
- [ ] Run `mvn test jacoco:report` (coverage)
- [ ] Manual testing: Create movement with category
- [ ] Manual testing: View balance history
- [ ] Manual testing: Import movements from file

### Documentation

- [ ] Update `docs/architecture/feature-structure.md`
- [ ] Update `docs/patterns/mapping-flow.md`
- [ ] Update `.claude/CLAUDE.md` (add CategoryVO example)

### Deployment

- [ ] Code review
- [ ] Merge to main
- [ ] Deploy to test environment
- [ ] Smoke test
- [ ] Deploy to production

---

## 7. Risk Assessment

| Risk                              | Probability | Impact | Mitigation                              |
|-----------------------------------|-------------|--------|-----------------------------------------|
| Breaking changes in other modules | Medium      | High   | Comprehensive search before refactoring |
| Test coverage gaps                | Low         | Medium | Write tests first (TDD approach)        |
| Performance regression            | Low         | Low    | Categories are lightweight VOs          |
| Lost business logic during merge  | Low         | High   | Thorough code review                    |
| Downstream service impacts        | Low         | Medium | Check API contracts                     |

---

## 8. Success Criteria

### Functional

- ✅ All existing functionality works
- ✅ No regression bugs
- ✅ Movement creation with categories works
- ✅ Balance calculations include category data

### Technical

- ✅ Naming follows standards (`CategoryVO`)
- ✅ `CategoryDTO` exists in application module
- ✅ Test coverage ≥ 50% (target 80%)
- ✅ Zero TODO/FIXME comments
- ✅ All JavaDoc present
- ✅ All tests pass

### Architectural

- ✅ Hexagonal architecture maintained
- ✅ Domain module has zero framework dependencies
- ✅ Proper VO → DTO → Response mapping
- ✅ Composition over inheritance

---

## 9. Future Considerations

### When to Migrate to Entity/Table

**Indicators**:

1. **User Request**: "I want to add my own categories"
2. **Scale**: More than 50 categories total
3. **Multi-tenancy**: Different categories per team
4. **Internationalization**: Support for 5+ languages
5. **Business Rules**: Category-specific calculation rules

### Migration Path (Enum → Entity)

**Phase 1**: Add database table (keep enums)

```sql
CREATE TABLE finance.movement_category
(
    category_key VARCHAR(50) PRIMARY KEY, -- matches enum.databaseKey
    -- other fields
);
```

**Phase 2**: Populate with enum values

```java
// Migration script
for(IncomeCategory category :IncomeCategory.

values()){
    categoryRepository.

save(new Category(category.getDatabaseKey(), ...));
    }
```

**Phase 3**: Switch factory to read from DB

```java
// CategoryFactory
public static CategoryVO fromDatabase(String key) {
  Category entity = categoryRepository.findByKey(key);
  return CategoryVO.fromEntity(entity);
}
```

**Phase 4**: Deprecate enums (keep for backward compatibility)

**Phase 5**: Remove enums (major version bump)

---

## 10. Estimated Timeline

| Phase                            | Duration               | Dependencies     |
|----------------------------------|------------------------|------------------|
| Phase 1: Preparation             | 4 hours                | None             |
| Phase 2: Domain Refactoring      | 6 hours                | Phase 1 complete |
| Phase 3: Application Integration | 8 hours                | Phase 2 complete |
| Phase 4: Testing & Validation    | 4 hours                | Phase 3 complete |
| Phase 5: Documentation           | 2 hours                | Phase 4 complete |
| **Total**                        | **24 hours** (~3 days) | Sequential       |

---

## 11. Recommendations

### Immediate Actions (This Sprint)

1. ✅ **Create unit tests** for current implementation
2. ✅ **Rename** `CategoryDomain` → `CategoryVO`
3. ✅ **Add** `CategoryDTO` to application module
4. ✅ **Remove** unprofessional comments
5. ✅ **Resolve** TODO comments

### Short-term (Next Sprint)

1. 🔄 **Refactor** `MovementCategoryVO` (merge or delete)
2. 🔄 **Create** `CategoryFactory` for static constants
3. 🔄 **Update** all documentation

### Long-term (Future)

1. 📅 **Monitor** category usage patterns
2. 📅 **Re-evaluate** enum vs entity decision in 6 months
3. 📅 **Prepare** migration path if user-defined categories needed

### DO NOT (Keep as Enum)

- ❌ Do NOT migrate to entity/table without business need
- ❌ Do NOT over-engineer with unnecessary abstraction
- ❌ Do NOT compromise performance for theoretical flexibility

---

## 12. Appendix

### A. Current Category Enums

**Income Categories** (10):

- TRANSFER, SALARY, DIVIDENDS, FREELANCE, INVESTMENT, RENTAL, GIFT, OTHER, INITIAL_BALANCE, DEPOSIT

**Expense Categories** (6):

- RETEFUENTE, SOCIAL_SECURITY, PUBLIC_SERVICES, PERSONAL, TRANSFER, INVESTMENT_WITHDRAWAL_TO_CLOSE_IT

### B. References

- Project naming standards: `docs/standards/naming.md`
- Hexagonal architecture: `docs/architecture/hexagonal-layers.md`
- Testing requirements: `docs/standards/testing.md`
- Code quality: `docs/standards/code-quality.md`

### C. Related Issues

- Missing test coverage for domain VOs
- Need for consistent VO naming across modules
- DTO mapping patterns need documentation update

---

**Document Status**: ✅ Ready for Review
**Next Action**: Team review and approval
**Owner**: Architecture Team
