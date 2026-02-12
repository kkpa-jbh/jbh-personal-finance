# Module Renaming Plan: jbh-products → jbh-finance

**Version:** 1.0.0
**Date:** 2026-02-07
**Status:** Ready for Execution
**Estimated Effort:** ~9 hours

---

## Executive Summary

**Recommendation:** Rename the `jbh-products` module to `jbh-finance` ✅

**Why:**

- The current name "jbh-products" only describes ONE of THREE features in the module
- Creates naming confusion: "product" feature within a "products" module
- API already uses `/jbh-api/finance` (not `/products`)
- Database schema already named `finance`
- "Finance" accurately describes the entire business domain: products, movements, and monthly balances

**Impact:** ~300+ Java files, 7 pom.xml files, 3 module-info.java files
**Breaking Changes:** None (API paths and database schema remain unchanged)

---

## Context

The **jbh-products** module currently manages three core business features:

1. **Product Management** - Financial accounts/products (savings, investments, loans, CDT, credit cards)
2. **Movement Management** - Financial transactions (deposits, withdrawals, transfers)
3. **Monthly Balance Tracking** - Aggregated monthly summaries and profit calculations

**The Problem:**

- Module name "jbh-products" suggests it only manages products/accounts
- However, it contains THREE distinct features (product, movement, monthlybalance)
- The name creates confusion: there's a "product" feature within a "products" module
- The REST API already uses `/jbh-api/finance` (NOT `/products`)
- The database schema is already named `finance`
- This is really a **Personal Finance Management** module, not just products

**User Consideration:**
Rename the entire module to better reflect its comprehensive scope.

---

## 📊 Naming Options Analysis

### Option 1: **jbh-finance** ✅ **RECOMMENDED**

**Pros:**

- ✅ **Already aligned with existing infrastructure:**
    - API paths: `/jbh-api/finance/*`
    - Database schema: `finance`
    - Configuration keys: `jbh.schema.name.products=finance`
- ✅ **Accurately describes the business domain:** Personal Finance Management
- ✅ **Broad enough to encompass all features:**
    - Financial products/accounts
    - Transactions/movements
    - Balance tracking and reporting
    - Future features (budgets, goals, analytics)
- ✅ **Clear and unambiguous:** Everyone understands "finance"
- ✅ **Removes naming conflict:** No more "product feature in products module"
- ✅ **Professional and scalable:** Works well for growth

**Cons:**

- ⚠️ Requires renaming ~300+ files (but this is manageable with tooling)

**New Structure:**

```
jbh-finance/
├── jbh-finance-domain/      (ProductDomain, MovementDomain, MonthlyBalanceDomain)
├── jbh-finance-application/  (feature/product, feature/movement, feature/monthlybalance)
└── jbh-finance-infra/       (REST, persistence, database adapters)
```

**Package Names:**

```java
com.jbh.finance.domain .*
com.jbh.finance.application.feature.product .*
com.jbh.finance.application.feature.movement .*
com.jbh.finance.application.feature.monthlybalance .*
com.jbh.finance.infra .*
```

**Module Names:**

```java
module jbh.finance.domain { ...
}
module jbh.finance.application{...}
    module jbh.finance.infra{...}
```

---

### Option 2: **jbh-portfolio**

**Pros:**

- ✅ Good for investment/asset management focus
- ✅ Modern, professional term
- ✅ Implies collection of financial instruments

**Cons:**

- ⚠️ **Too narrow:** "Portfolio" is primarily investment-focused
    - Doesn't capture everyday banking transactions
    - Doesn't fit loans, credit cards as well
    - Less inclusive of general personal finance
- ⚠️ **Inconsistent with existing infrastructure:**
    - API is `/finance`, not `/portfolio`
    - Schema is `finance`, not `portfolio`
    - Would create NEW inconsistencies

**Verdict:** Good name, but creates more problems than it solves. Only choose this if you want to rebrand the entire product as investment-focused.

---

### Option 3: **jbh-ledger**

**Pros:**

- ✅ Accurate: It IS a financial ledger
- ✅ Technical precision

**Cons:**

- ⚠️ **Too technical:** "Ledger" is accounting jargon
- ⚠️ **Less user-friendly:** Doesn't communicate to non-accountants
- ⚠️ **Doesn't match existing API/schema naming**

**Verdict:** Technically correct but not recommended for user-facing product.

---

### Option 4: **jbh-accounts**

**Pros:**

- ✅ Clear and simple
- ✅ User-friendly

**Cons:**

- ⚠️ **Too narrow:** Only describes one feature (products/accounts)
- ⚠️ **Ignores movements and balances**
- ⚠️ **Same problem as "products"** - just a different narrow term

**Verdict:** Not recommended - doesn't solve the original problem.

---

## 🎯 Final Recommendation

### **Choose: jbh-finance** ✅

**Why:**

1. **Consistency:** Aligns with existing API (`/finance`) and schema (`finance`)
2. **Comprehensive:** Covers all three features naturally
3. **Clarity:** Removes the "product in products" naming conflict
4. **Future-proof:** Can easily add budgets, goals, analytics, reports under "finance"
5. **Professional:** Standard industry terminology
6. **Low conceptual overhead:** Everyone understands what "finance" means

**Trade-offs:**

- Requires systematic rename of 300+ files (but this is a one-time cost)
- Temporary disruption during migration (mitigated with good planning)
- Worth it for long-term clarity and maintainability

---

## 📋 What Would Need to Change

### 1. **Maven Module Structure** (3 modules)

```xml
<!-- Root pom.xml -->
<modules>
  <module>jbh-finance</module>  <!-- was: jbh-products -->
</modules>

<dependencyManagement>
<dependencies>
  <dependency>
    <groupId>com.jbh</groupId>
    <artifactId>jbh-finance-domain</artifactId>      <!-- was: jbh-products-domain -->
    <artifactId>jbh-finance-application</artifactId>  <!-- was: jbh-products-application -->
    <artifactId>jbh-finance-infra</artifactId>        <!-- was: jbh-products-infra -->
  </dependency>
</dependencies>
</dependencyManagement>
```

**Files affected:**

- `pom.xml` (root)
- `jbh-products/pom.xml` → `jbh-finance/pom.xml`
- `jbh-products/jbh-products-domain/pom.xml` → `jbh-finance/jbh-finance-domain/pom.xml`
- `jbh-products/jbh-products-application/pom.xml` → `jbh-finance/jbh-finance-application/pom.xml`
- `jbh-products/jbh-products-infra/pom.xml` → `jbh-finance/jbh-finance-infra/pom.xml`
- `jbh-z-assembly/pom.xml` (dependency reference)

---

### 2. **Java Package Names** (ALL ~300+ Java files)

**Before:**

```java
package com.jbh.products.domain.product;
package com.jbh.products.application.feature.product.usecases;
package com.jbh.products.infra.adapters.in.rest.product;
```

**After:**

```java
package com.jbh.finance.domain.product;
package com.jbh.finance.application.feature.product.usecases;
package com.jbh.finance.infra.adapters.in.rest.product;
```

**Affected files:**

- `jbh-finance-domain/`: ~50 files
- `jbh-finance-application/`: ~150 files
- `jbh-finance-infra/`: ~100 files
- **All imports** in these files (cross-references between modules)

---

### 3. **Module-info.java Files** (3 files)

**Before:**

```java
module jbh.products.domain {
  requires jbh.commons;
  exports com.jbh.products.domain.product;
  exports com.jbh.products.domain.movement;
  // ...
}

module jbh.products.application{
    requires jbh.products.domain;
    exports com.jbh.products.application.feature.product.dto;
    // ...
    }

    module jbh.products.infra{
    requires jbh.products.application;
    requires jbh.products.domain;
    exports com.jbh.products.infra.adapters.in.rest;
    // ...
    }
```

**After:**

```java
module jbh.finance.domain {
  requires jbh.commons;
  exports com.jbh.finance.domain.product;
  exports com.jbh.finance.domain.movement;
  // ...
}

module jbh.finance.application{
    requires jbh.finance.domain;
    exports com.jbh.finance.application.feature.product.dto;
    // ...
    }

    module jbh.finance.infra{
    requires jbh.finance.application;
    requires jbh.finance.domain;
    exports com.jbh.finance.infra.adapters.in.rest;
    // ...
    }
```

**Files affected:**

- `jbh-finance-domain/src/main/java/module-info.java`
- `jbh-finance-application/src/main/java/module-info.java`
- `jbh-finance-infra/src/main/java/module-info.java`

---

### 4. **Configuration Files**

**application.properties (jbh-finance-infra):**

```properties
# Schema name (OPTIONAL - could keep as "finance" or rename property)
jbh.schema.name.products=finance  # Consider: jbh.schema.name.finance=finance
# Hibernate packages
quarkus.hibernate-orm.finance.packages=com.jbh.finance.infra.adapters.out.persistence
# Liquibase
quarkus.liquibase.finance.migrate-at-start=true
```

**Files affected:**

- `jbh-finance-infra/src/main/resources/application.properties`
- `jbh-finance-infra/src/main/resources/application-dev.properties`
- `jbh-z-assembly/src/main/resources/application.properties`

---

### 5. **Directory Structure**

**Physical rename:**

```bash
# Root level
mv jbh-products/ jbh-finance/

# Submodules
mv jbh-finance/jbh-products-domain/ jbh-finance/jbh-finance-domain/
mv jbh-finance/jbh-products-application/ jbh-finance/jbh-finance-application/
mv jbh-finance/jbh-products-infra/ jbh-finance/jbh-finance-infra/

# Package structure
mv .../com/jbh/products/ .../com/jbh/finance/
```

---

### 6. **Test Files** (ALL test packages)

**Before:**

```java
package com.jbh.products.application.core.usecases.integration.movements;

import com.jbh.products.application.feature.product.dto.ProductDTO;
```

**After:**

```java
package com.jbh.finance.application.core.usecases.integration.movements;

import com.jbh.finance.application.feature.product.dto.ProductDTO;
```

**Files affected:**

- `jbh-finance-domain/src/test/java/com/jbh/finance/**/*`
- `jbh-finance-application/src/test/java/com/jbh/finance/**/*`
- `jbh-finance-infra/src/test/java/com/jbh/finance/**/*`

---

### 7. **Database & Liquibase** (OPTIONAL - probably keep as-is)

**Current:**

- Schema name: `finance` (GOOD - no need to change)
- Master changelog: `/db/finance-db-master.xml` (GOOD - no need to change)
- Tables: `finance.products`, `finance.movements`, `finance.monthly_balances`

**Recommendation:** **Keep database naming as-is.** The schema is already called `finance`, which is perfect. No need to rename tables or migrate data.

---

### 8. **API Routes** (NO CHANGE NEEDED)

**Current:**

```java
public static final String BASE_API_PATH = "/jbh-api/finance";
```

**Recommendation:** **Keep API paths unchanged.** They already use `/finance`, which is correct. Changing API paths would break existing clients.

---

### 9. **Consul Service Registration** (OPTIONAL)

**Current:** `jbh-personal-finance` (registered at assembly level)

**Options:**

1. **Keep as-is:** `jbh-personal-finance` (RECOMMENDED - no breaking change)
2. **Rename to:** `jbh-finance-service` (more specific)

**Recommendation:** Keep as-is unless you're rebranding the entire application.

---

### 10. **Documentation & Comments**

**Files to review:**

- README.md files
- JavaDoc comments referencing "products module"
- Architecture diagrams
- `.claude/CLAUDE.md` (already updated with feature-based structure)

---

## 🚨 Risks & Mitigations

| Risk                         | Impact                      | Mitigation                                            |
|------------------------------|-----------------------------|-------------------------------------------------------|
| **Broken imports**           | Build failures              | Use IDE refactoring tools (IntelliJ "Rename Package") |
| **Missed references**        | Runtime errors              | Comprehensive grep search for "products" references   |
| **Test failures**            | CI/CD breaks                | Run full test suite after each major change           |
| **Module visibility issues** | Compilation errors          | Carefully update all module-info.java files           |
| **Git history loss**         | Harder to trace changes     | Use `git mv` for renames to preserve history          |
| **Team confusion**           | Temporary productivity loss | Clear communication, documentation updates            |
| **External integrations**    | API client breaks           | NO - API paths remain unchanged                       |
| **Database migrations**      | Data loss                   | NO - schema and tables remain unchanged               |

---

## 📝 Implementation Plan

### Phase 1: Preparation (Read-Only)

1. ✅ Create backup branch
2. ✅ Document all "products" references
3. ✅ Run full test suite to establish baseline
4. ✅ Communicate with team

### Phase 2: Maven & Build Structure

1. Rename physical directories:
    - `jbh-products/` → `jbh-finance/`
    - `jbh-products-domain/` → `jbh-finance-domain/`
    - `jbh-products-application/` → `jbh-finance-application/`
    - `jbh-products-infra/` → `jbh-finance-infra/`
2. Update all `pom.xml` files:
    - Root pom: module declaration, dependency management
    - Module poms: artifactId, parent reference
    - Assembly pom: dependency reference
3. Verify Maven structure: `mvn clean verify -DskipTests`

### Phase 3: Java Package Renaming (Domain Module)

1. Use IDE refactoring: Rename package `com.jbh.products` → `com.jbh.finance` in domain module
2. Update `module-info.java` for domain module
3. Compile domain: `mvn clean compile -pl jbh-finance-domain`
4. Run domain tests: `mvn test -pl jbh-finance-domain`

### Phase 4: Java Package Renaming (Application Module)

1. Use IDE refactoring: Rename package in application module
2. Update imports from domain module
3. Update `module-info.java` for application module
4. Compile application: `mvn clean compile -pl jbh-finance-application -am`
5. Run application tests: `mvn test -pl jbh-finance-application`

### Phase 5: Java Package Renaming (Infra Module)

1. Use IDE refactoring: Rename package in infra module
2. Update imports from domain and application modules
3. Update `module-info.java` for infra module
4. Update `application.properties` (hibernate packages property)
5. Compile infra: `mvn clean compile -pl jbh-finance-infra -am`
6. Run infra tests: `mvn test -pl jbh-finance-infra`

### Phase 6: Assembly & Full Integration

1. Update `jbh-z-assembly/pom.xml` dependency references
2. Full clean build: `mvn clean install`
3. Run all tests: `mvn verify`
4. Start application and verify:
    - API endpoints respond correctly
    - Database connections work
    - Consul registration succeeds

### Phase 7: Documentation & Cleanup

1. Update README.md files
2. Update `.claude/CLAUDE.md` (module references)
3. Search and replace remaining "products" references in comments
4. Update architecture diagrams (if any)
5. Git commit with descriptive message

### Phase 8: Verification & Rollout

1. ✅ Full test suite passes
2. ✅ Application starts successfully
3. ✅ API smoke tests pass
4. ✅ Database schema intact
5. ✅ Code coverage maintained
6. ✅ No "products" references in package names
7. ✅ Git history preserved (used `git mv`)

---

## 🎯 Critical Files Checklist

### Maven/Build (7 files)

- [ ] `pom.xml` (root)
- [ ] `jbh-finance/pom.xml`
- [ ] `jbh-finance/jbh-finance-domain/pom.xml`
- [ ] `jbh-finance/jbh-finance-application/pom.xml`
- [ ] `jbh-finance/jbh-finance-infra/pom.xml`
- [ ] `jbh-z-assembly/pom.xml`
- [ ] `.github/workflows/*.yml` (CI/CD, if any)

### Module System (3 files)

- [ ] `jbh-finance-domain/src/main/java/module-info.java`
- [ ] `jbh-finance-application/src/main/java/module-info.java`
- [ ] `jbh-finance-infra/src/main/java/module-info.java`

### Configuration (3 files)

- [ ] `jbh-finance-infra/src/main/resources/application.properties`
- [ ] `jbh-finance-infra/src/main/resources/application-dev.properties`
- [ ] `jbh-z-assembly/src/main/resources/application.properties`

### Java Packages (~300+ files)

- [ ] All files in `jbh-finance-domain/src/main/java/com/jbh/finance/**/*`
- [ ] All files in `jbh-finance-application/src/main/java/com/jbh/finance/**/*`
- [ ] All files in `jbh-finance-infra/src/main/java/com/jbh/finance/**/*`

### Test Packages (~150+ files)

- [ ] All files in `jbh-finance-domain/src/test/java/com/jbh/finance/**/*`
- [ ] All files in `jbh-finance-application/src/test/java/com/jbh/finance/**/*`
- [ ] All files in `jbh-finance-infra/src/test/java/com/jbh/finance/**/*`

---

## 📊 Estimated Effort

| Phase                           | Complexity | Time Estimate |
|---------------------------------|------------|---------------|
| Phase 1: Preparation            | Low        | 30 minutes    |
| Phase 2: Maven Structure        | Medium     | 1 hour        |
| Phase 3: Domain Module          | Medium     | 1 hour        |
| Phase 4: Application Module     | High       | 2 hours       |
| Phase 5: Infra Module           | High       | 2 hours       |
| Phase 6: Assembly & Integration | Medium     | 1 hour        |
| Phase 7: Documentation          | Low        | 30 minutes    |
| Phase 8: Verification           | Medium     | 1 hour        |
| **Total**                       | **High**   | **~9 hours**  |

**Note:** With IDE automation (IntelliJ's "Rename Package"), most Java file changes are automated, reducing manual effort significantly.

---

## ✅ Success Criteria

After the rename is complete, verify:

1. ✅ **Build Success**: `mvn clean install` completes without errors
2. ✅ **All Tests Pass**: `mvn verify` shows 100% test success rate
3. ✅ **Application Starts**: Quarkus starts without errors
5. ✅ **No "products" in packages**: `grep -r "com.jbh.products" src/` returns empty
6. ✅ **Module System Valid**: `javac --module-path` recognizes all modules
8. ✅ **Database Intact**: Schema and data unchanged
9. ✅ **Documentation Updated**: README and CLAUDE.md reflect new naming

---

## 🎯 Recommendation Summary

### **RENAME TO: jbh-finance** ✅

**Rationale:**

- Aligns with existing API (`/finance`) and database (`finance` schema)
- Accurately describes the full business domain (not just products)
- Removes the "product in products" naming confusion
- Professional, clear, and future-proof
- Standard industry terminology

**Key Advantages:**

1. **Consistency** with existing infrastructure
2. **Clarity** for developers and users
3. **Scalability** for future features (budgets, goals, analytics)
4. **No breaking changes** to API or database

**Trade-offs:**

- One-time cost of renaming ~300+ files
- ~9 hours of careful refactoring work
- Temporary team adjustment period

**Verdict:** **Worth it.** The long-term benefits of clarity and consistency far outweigh the one-time migration cost.

---

## Execution Notes

**Prerequisites:**

- Create a feature branch: `git checkout -b refactor/rename-to-jbh-finance`
- Ensure no uncommitted changes
- Run baseline test suite: `mvn clean verify`
- Communicate with team about the upcoming change

**Recommended Tools:**

- IntelliJ IDEA's "Rename Package" refactoring (highly automated)
- `git mv` for all file/directory moves (preserves history)
- `sed` or `perl` for batch text replacements
- `grep -r` for verification searches

**Post-Execution:**

- Create comprehensive git commit message explaining the rename
- Update this plan document with actual execution notes
- Document any unexpected issues encountered
- Share lessons learned with the team

---

**Plan Status:** Ready for execution when approved
**Last Updated:** 2026-02-07
