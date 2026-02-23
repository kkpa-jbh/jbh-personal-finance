# Entity-Based Category Migration Plan

## Context

### Why This Change Is Needed

The current category implementation uses Java ENUMs (IncomeCategory, ExpenseCategory) with categories stored as TEXT values in the database. While this approach provides compile-time type safety, it has significant limitations:

1. **Requires Code Deployment** - Adding/modifying categories requires code changes and redeployment
2. **Not User-Configurable** - Users cannot create custom categories
3. **Limited Flexibility** - Cannot disable/enable categories dynamically
4. **No Audit Trail** - Cannot track when categories were added/removed/modified
5. **Hardcoded Translations** - Cannot update translations without deployment

The user has decided to migrate to an **entity-based approach** using database tables, which provides:

- Dynamic category management (no code deployment needed)
- Soft delete support (is_active flag)
- Future extensibility (user-defined categories, metadata, hierarchies)
- Audit trail capabilities
- Centralized reference data management

### Current State

**Enums (To Be Replaced)**:
- `IncomeCategory` - 10 categories: TRANSFER, SALARY, DIVIDENDS, FREELANCE, INVESTMENT, RENTAL, GIFT, OTHER, INITIAL_BALANCE, DEPOSIT
- `ExpenseCategory` - 6 categories: RETEFUENTE, SOCIAL_SECURITY, PUBLIC_SERVICES, PERSONAL, TRANSFER, INVESTMENT_WITHDRAWAL_TO_CLOSE_IT

**Database**:
- Movement table has `category_type TEXT NULL` column storing enum names
- No category reference table exists
- No foreign key constraints on categories

**Translation Handling**:
- Translations embedded in enum constructors as bilingual JSON: `{"en":"Salary","es":"Salario"}`
- Generated via `JbhStringUtils.buildJsonMessage(en, es)`
- Exposed via REST API in CategoryResponse

**User Requirements**:
- Keep translations in Java code (not in database)
- Use `CategoryTranslationRegistry` pattern to centralize translations
- Use `finance.categories` table name (plural, following existing pattern: products, movements, monthly_balances)

---

## Solution Architecture

### Database Schema

Create `finance.categories` table with composite natural key:

```sql
CREATE TABLE finance.categories (
    category_key    TEXT NOT NULL,      -- "SALARY", "TRANSFER", etc.
    category_source TEXT NOT NULL,      -- "INCOME" or "EXPENSE"
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_categories PRIMARY KEY (category_key, category_source),
    CONSTRAINT chk_category_source_valid CHECK (category_source IN ('INCOME', 'EXPENSE'))
);
```

**Key Design Decisions**:
- **Table Name**: `categories` (plural, matches existing pattern: `products`, `movements`, `monthly_balances`)
- **Composite Primary Key**: `(category_key, category_source)` - allows "TRANSFER" in both INCOME and EXPENSE
- **No UUID**: Categories are reference data with natural keys
- **Soft Delete**: `is_active` flag preserves historical data
- **Display Order**: Supports UI ordering without code changes

### JPA Entity Pattern

Use `@IdClass` for composite primary key:

```java
@Entity
@IdClass(CategoryId.class)
@Table(name = "categories", schema = "finance")
public class CategoryJPAEntity extends PanacheEntityBase {

  @Id
  @Column(name = "category_key")
  private String categoryKey;

  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "category_source")
  private CategorySource categorySource;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "display_order")
  private Integer displayOrder;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
```

### Translation Registry (Java-Based)

Keep translations in code per user requirement:

```java
public final class CategoryTranslationRegistry {

  private static final Map<String, String> INCOME_TRANSLATIONS = Map.ofEntries(
      Map.entry("SALARY", JbhStringUtils.buildJsonMessage("Salary", "Salario")),
      // ... all 10 income categories
  );

  private static final Map<String, String> EXPENSE_TRANSLATIONS = Map.ofEntries(
      Map.entry("PERSONAL", JbhStringUtils.buildJsonMessage("Personal", "Personal")),
      // ... all 6 expense categories
  );

  public static String getTranslation(String categoryKey, CategorySource source) {
    // Returns bilingual JSON from registry
  }
}
```

### Repository Pattern

Quarkus Panache repository with caching:

```java
@ApplicationScoped
@PersistenceUnit(name = "finance")
public class CategoryJPARepository implements PanacheRepository<CategoryJPAEntity> {

  @CacheResult(cacheName = "category-by-key")
  public Optional<CategoryJPAEntity> findByKeyAndSource(String key, CategorySource source) {
    // Query by composite key
  }

  @CacheResult(cacheName = "categories-by-source")
  public List<CategoryJPAEntity> findAllBySource(CategorySource source) {
    // Query active categories, ordered by display_order
  }
}
```

---

## Critical Files

### New Files to Create

**1. Database Migration**
- Path: `jbh-finance-infra/src/main/resources/db/changelog/002_create_categories_table.sql`
- Creates category table, indexes, seeds 16 categories
- Validation: Ensures all existing movement categories are valid

**2. Composite Key Class**
- Path: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/out/persistence/category/CategoryId.java`
- Implements Serializable for @IdClass
- Equals/hashCode based on (categoryKey, categorySource)

**3. JPA Entity**
- Path: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/out/persistence/category/CategoryJPAEntity.java`
- Extends PanacheEntityBase
- Composite key via @IdClass
- Bidirectional mapping: toEntity(), toDTO()

**4. JPA Repository**
- Path: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/out/persistence/category/CategoryJPARepository.java`
- Implements PanacheRepository<CategoryJPAEntity>
- Methods: findByKeyAndSource, findAllBySource, findAllActive
- Caching via @CacheResult

**5. Translation Registry**
- Path: `jbh-finance-domain/src/main/java/com/jbh/finance/domain/movement/CategoryTranslationRegistry.java`
- Static maps for INCOME_TRANSLATIONS, EXPENSE_TRANSLATIONS
- Replaces translations from enum constructors
- Method: getTranslation(key, source)

**6. Application DTO**
- Path: `jbh-finance-application/src/main/java/com/jbh/finance/application/feature/category/dto/CategoryDTO.java`
- Record with: categoryKey, categorySource, isActive, displayOrder, createdAt
- Builder pattern

### Files to Modify

**7. CategoryDomain.java** (domain entity)
- Path: `jbh-finance-domain/src/main/java/com/jbh/finance/domain/movement/CategoryDomain.java`
- Replace `CategoryType categoryType` with `String categoryKey + CategorySource categorySource`
- Add `fromDTO(CategoryDTO)` factory method
- Update `getTranslationKey()` to use CategoryTranslationRegistry

**8. MovementCategoryVO.java** (value object)
- Path: `jbh-finance-domain/src/main/java/com/jbh/finance/domain/movement/vo/MovementCategoryVO.java`
- Update constructor to accept `(String categoryKey, CategorySource categorySource)`
- Update `withName()` to work with string keys instead of enums

**9. CategoryResponse.java** (REST response)
- Path: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/rest/category/response/CategoryResponse.java`
- Add `fromDTO(CategoryDTO)` method
- Add `fromDomain(CategoryDomain)` method
- Keep existing `fromDTO(CategoryType)` temporarily for backward compatibility

**10. IncomeCategoryRestAdapter.java** (REST endpoint)
- Path: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/rest/category/IncomeCategoryRestAdapter.java`
- Inject `CategoryJPARepository`
- Replace `Arrays.stream(IncomeCategory.values())` with `categoryRepository.findAllBySource(CategorySource.INCOME)`

**11. ExpenseCategoryRestAdapter.java** (REST endpoint)
- Path: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/rest/category/ExpenseCategoryRestAdapter.java`
- Inject `CategoryJPARepository`
- Replace `Arrays.stream(ExpenseCategory.values())` with `categoryRepository.findAllBySource(CategorySource.EXPENSE)`

**12. MovementJPAEntity.java** (persistence)
- Path: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/out/persistence/movement/MovementJPAEntity.java`
- Update `toDTO()` to use CategoryLookupService or CategoryDomain.fromDTO()
- Keep existing `category_type` column (minimal change approach)
- Future: Add FK constraint after validation

**13. MovementDomain.java** (domain logic)
- Path: `jbh-finance-domain/src/main/java/com/jbh/finance/domain/movement/MovementDomain.java`
- Update `withFileImport()` to query repository instead of using enum constants
- Replace `IncomeCategory.OTHER` with repository lookup
- Replace `ExpenseCategory.PERSONAL` with repository lookup

### Files to Deprecate (Later Phase)

**14. CategoryType.java** (interface)
- Path: `jbh-finance-domain/src/main/java/com/jbh/finance/domain/movement/vo/CategoryType.java`
- Add `@Deprecated` annotation
- Keep for backward compatibility during migration

**15. IncomeCategory.java** (enum)
- Path: `jbh-finance-domain/src/main/java/com/jbh/finance/domain/movement/vo/IncomeCategory.java`
- Add `@Deprecated` annotation
- Delete in final cleanup phase

**16. ExpenseCategory.java** (enum)
- Path: `jbh-finance-domain/src/main/java/com/jbh/finance/domain/movement/vo/ExpenseCategory.java`
- Add `@Deprecated` annotation
- Delete in final cleanup phase

---

## Implementation Steps

### Phase 1: Database & Infrastructure (Non-Breaking)

**Step 1.1: Create Migration Script**

Create file: `jbh-finance-infra/src/main/resources/db/changelog/002_create_categories_table.sql`

```sql
-- Create categories table
CREATE TABLE finance.categories (
    category_key    TEXT NOT NULL,
    category_source TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order   INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT pk_categories PRIMARY KEY (category_key, category_source),
    CONSTRAINT chk_category_source_valid CHECK (category_source IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_category_key_not_empty CHECK (length(trim(category_key)) > 0)
);

-- Indexes
CREATE INDEX idx_categories_source ON finance.categories (category_source, is_active);
CREATE INDEX idx_categories_order ON finance.categories (category_source, display_order);

-- Seed income categories (10)
INSERT INTO finance.categories (category_key, category_source, display_order) VALUES
    ('TRANSFER', 'INCOME', 1),
    ('SALARY', 'INCOME', 2),
    ('DIVIDENDS', 'INCOME', 3),
    ('FREELANCE', 'INCOME', 4),
    ('INVESTMENT', 'INCOME', 5),
    ('RENTAL', 'INCOME', 6),
    ('GIFT', 'INCOME', 7),
    ('OTHER', 'INCOME', 8),
    ('INITIAL_BALANCE', 'INCOME', 9),
    ('DEPOSIT', 'INCOME', 10);

-- Seed expense categories (6)
INSERT INTO finance.categories (category_key, category_source, display_order) VALUES
    ('RETEFUENTE', 'EXPENSE', 1),
    ('SOCIAL_SECURITY', 'EXPENSE', 2),
    ('PUBLIC_SERVICES', 'EXPENSE', 3),
    ('PERSONAL', 'EXPENSE', 4),
    ('TRANSFER', 'EXPENSE', 5),
    ('INVESTMENT_WITHDRAWAL_TO_CLOSE_IT', 'EXPENSE', 6);

-- Validation: Should be 16 categories
DO $$
DECLARE
    category_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO category_count FROM finance.categories;
    IF category_count != 16 THEN
        RAISE EXCEPTION 'Expected 16 categories, found %', category_count;
    END IF;
END $$;

-- Verify existing movement categories are valid
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM finance.movements m
    WHERE m.category_type IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM finance.categories c
          WHERE c.category_key = m.category_type
      );

    IF invalid_count > 0 THEN
        RAISE WARNING 'Found % movements with invalid category references', invalid_count;
    ELSE
        RAISE NOTICE 'All existing movement categories are valid';
    END IF;
END $$;
```

**Step 1.2: Update Liquibase Master File**

Add to `jbh-finance-infra/src/main/resources/db/finance-db-master.xml`:

```xml
<include file="db/changelog/002_create_categories_table.sql"/>
```

**Step 1.3-1.7**: Create CategoryId, CategoryJPAEntity, CategoryJPARepository, CategoryTranslationRegistry, CategoryDTO

(See detailed code examples in "Solution Architecture" section above)

**Verification**:
```bash
cd jbh-finance
./mvnw quarkus:dev

# Check table exists and has 16 categories
psql -d jbh_finance -c "SELECT category_source, COUNT(*) FROM finance.categories GROUP BY category_source;"
# Expected: INCOME = 10, EXPENSE = 6
```

---

### Phase 2-6: Remaining Implementation

(See full implementation steps for REST adapters, domain layer, movement integration, testing, and cleanup in the original sections)

---

## Verification Steps

### Database Verification

```bash
# Check table exists
psql -d jbh_finance -c "\d finance.categories"

# Check row count
psql -d jbh_finance -c "SELECT COUNT(*) FROM finance.categories;"
# Expected: 16

# Check distribution
psql -d jbh_finance -c "SELECT category_source, COUNT(*) FROM finance.categories GROUP BY category_source;"
# Expected: INCOME = 10, EXPENSE = 6

# Verify TRANSFER exists in both sources
psql -d jbh_finance -c "SELECT * FROM finance.categories WHERE category_key = 'TRANSFER';"
# Expected: 2 rows (INCOME, EXPENSE)
```

### API Testing

```bash
# Test endpoints
curl http://localhost:8080/api/v1/finance/categories/income | jq 'length'
# Expected: 10

curl http://localhost:8080/api/v1/finance/categories/expense | jq 'length'
# Expected: 6
```

---

## Success Criteria

### Functional
- ✅ All 16 categories stored in `finance.categories` table
- ✅ GET /categories/income returns 10 categories
- ✅ GET /categories/expense returns 6 categories
- ✅ TRANSFER appears in both income and expense lists with different sources
- ✅ Movement creation works with new categories
- ✅ Translations match original enum values
- ✅ Balance snapshots (no category) work correctly

### Non-Functional
- ✅ Response time < 100ms for category queries (with cache)
- ✅ Zero data loss during migration
- ✅ Code coverage > 50%
- ✅ All existing tests pass
- ✅ No breaking changes to API response structure

### Technical
- ✅ Table name follows plural pattern (`categories`)
- ✅ Hexagonal architecture maintained
- ✅ Quarkus Panache repository pattern followed
- ✅ Composite primary key `(category_key, category_source)` works correctly
- ✅ Caching reduces database queries
- ✅ Translations kept in Java code (not database)

---

## Rollback Strategy

### Database Rollback

```sql
-- Drop category table
DROP INDEX IF EXISTS finance.idx_categories_order;
DROP INDEX IF EXISTS finance.idx_categories_source;
DROP TABLE IF EXISTS finance.categories CASCADE;
```

---

## Future Enhancements

### 1. User-Defined Categories

```sql
ALTER TABLE finance.categories
ADD COLUMN user_id UUID NULL,
ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT TRUE;
```

### 2. Category Metadata

```sql
ALTER TABLE finance.categories
ADD COLUMN metadata JSONB;
```

### 3. Category Hierarchies

```sql
ALTER TABLE finance.categories
ADD COLUMN parent_category_key TEXT NULL,
ADD COLUMN parent_category_source TEXT NULL;
```

---

## Timeline Estimate

- **Phase 1** (Database & Infrastructure): 3-4 hours
- **Phase 2** (REST Adapters): 2-3 hours
- **Phase 3** (Domain Layer): 3-4 hours
- **Phase 4** (Movement Integration): 3-4 hours
- **Phase 5** (Testing & Validation): 3-4 hours
- **Phase 6** (Cleanup & Documentation): 2-3 hours

**Total**: 16-22 hours (2-3 days)

---

## Notes

- **Table Name**: `finance.categories` (plural, matches project pattern)
- **Translations**: Stay in Java code (`CategoryTranslationRegistry`)
- **Composite Key**: `(category_key, category_source)` allows duplicate names across sources
- **Caching**: `@CacheResult` improves performance
- **Migration Path**: Minimal changes to movement table (Phase 4)
- **Future**: Add FK constraint and normalize schema
