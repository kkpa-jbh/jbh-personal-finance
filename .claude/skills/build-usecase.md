# Build Use Case Skill

## Description

Interactively builds a complete use case following the jbh-finance hexagonal architecture patterns.

This skill guides you through creating:

- Use Case Interface (with JavaDoc)
- Input Port Implementation
- Output Port (if needed)
- Command Object (if needed)
- Repository Query (if needed)
- REST Endpoint
- Request/Response objects
- CDI Configuration
- Unit Tests (50%+ coverage)

## Usage

```
/build-usecase
```

## Instructions

You are helping the user build a complete use case for the jbh-finance modular monolith project following hexagonal architecture principles.

### Step 1: Read Architecture Patterns

First, read the complete use case pattern file:

```
/.claude/patterns/use-case-complete.md
```

This file contains all the patterns, naming conventions, templates, and a complete 12-layer implementation example.

**Key References:**
- [Naming Conventions](../standards/naming.md) - Complete naming patterns for all components
- [Code Quality Standards](../standards/code-quality.md) - PMD rules and SOLID principles
- [JavaDoc Templates](../standards/javadoc.md) - Use case documentation standards

### Step 2: Gather Requirements

Ask the user for the following information using the AskUserQuestion tool:

**Question 1: Use Case Type**

- Header: "Use Case Type"
- Question: "What type of use case are you building?"
- Options:
    - **Query (Read)**: Retrieve data (e.g., Find, Get, List, Search)
        - Description: "Read-only operations that query data from the database"
    - **Command (Write)**: Modify data (e.g., Create, Update, Delete, Add, Register)
        - Description: "Operations that create, update, or delete data"

**Question 2: Feature Domain**

- Header: "Feature"
- Question: "Which feature domain does this use case belong to?"
- Options:
    - **movement**: Movement/Transaction operations
        - Description: "Financial movements, transactions, deposits, withdrawals"
    - **product**: Product/Account operations
        - Description: "Financial products, accounts, CDTs, savings"
    - **monthlybalance**: Monthly balance operations
        - Description: "Monthly reports, balances, profit calculations"
    - **category**: Category operations
        - Description: "Income/expense categories"

**Question 3: Use Case Name**

- Ask: "What action does this use case perform? (e.g., FindMovements, CreateProduct, RegisterBalance)"
- Format: `<Action><Entity>` (e.g., FindMovementsByProduct, DeleteProduct)

**Question 4: Needs New Repository Method**

- Header: "Repository"
- Question: "Does this use case need a new repository method?"
- Options:
    - **Yes**: Will create new query/write method
        - Description: "New database query or persistence operation needed"
    - **No**: Uses existing repository methods
        - Description: "Existing repository methods are sufficient"

**Question 5: Needs Command Object**

- Header: "Command"
- Question: "Does this use case need a command object for input?"
- Options:
    - **Yes**: Will create command with validation
        - Description: "Complex input with multiple fields and validation rules"
    - **No**: Simple parameters (UUID, ProductId, etc.)
        - Description: "Simple inputs, no complex validation needed"

### Step 3: Plan the Implementation

Based on the gathered information, create a task list with TaskCreate:

1. **Application Layer Tasks:**
    - Update Use Case Interface (if exists) or create new
    - Create Input Port Implementation
    - Create Command Object (if needed)
    - Create/Update Output Port (if repository needed)
    - Create Unit Tests

2. **Infrastructure Layer Tasks:**
    - Update/Create Repository Adapter (if needed)
    - Update/Create JPA Repository (if needed)
    - Update/Create REST Adapter
    - Create Request Object (for Command use cases)
    - Create Response Object (for Query use cases)
    - Update CDI Configuration

### Step 4: Build Application Layer

Follow the templates from `patterns/use-case-complete.md` for each component.

**Reference:** See [Naming Conventions](../standards/naming.md) for complete naming patterns.

#### 4.1 Use Case Interface

See `use-case-complete.md` section 1 for the full template with JavaDoc structure.

**Key Requirements:**
- Class-level JavaDoc: Technical purpose, User Explanation, Business Rules
- Method-level JavaDoc: Validations, Database Operations
- Full documentation template in [JavaDoc Standards](../standards/javadoc.md)

#### 4.2 Input Port Implementation

See `use-case-complete.md` section 2 for complete pattern.

**Key Points:**
- NO annotations on class or constructor
- All fields final
- Validation first, then business logic
- Delegates to services

#### 4.3 Command Object (if needed)

See `use-case-complete.md` section 5 for command pattern with builder.

#### 4.4 Output Port (if needed)

See `use-case-complete.md` section 3 for repository interface pattern.

#### 4.5 Unit Tests

Follow testing patterns from [Testing Standards](../standards/testing.md).

**Tests to include:**
- Happy path
- Validation errors
- Business exceptions
- Edge cases

### Step 5: Build Infrastructure Layer

#### 5.1-5.6 Infrastructure Components

See `use-case-complete.md` sections 6-12 for complete templates:
- Section 6: Repository Adapter
- Section 7: JPA Repository
- Section 8: JPA Entity
- Section 9: REST Adapter
- Section 10: Request Object
- Section 11: Response Object
- Section 12: CDI Configuration

**Naming Reference:** All component naming follows [Naming Conventions](../standards/naming.md)

### Step 6: Implementation Order

**CRITICAL**: Implement files in this order (see `use-case-complete.md` for details):

1. ✅ Use Case Interface (application)
2. ✅ Command Object (if needed, application)
3. ✅ Output Port Interface (if needed, application)
4. ✅ Input Port Implementation (application)
5. ✅ Repository Adapter (if needed, infra)
6. ✅ JPA Repository (if needed, infra)
7. ✅ REST Adapter method (infra)
8. ✅ Request/Response objects (infra)
9. ✅ CDI Configuration (infra)
10. ✅ Unit Tests (application)
11. ✅ Run tests to verify

### Step 7: Validation & Testing

After creating all files:

1. **Compile Application Module**:
   ```bash
   mvn clean compile -pl jbh-finance/jbh-finance-application -am -DskipTests
   ```

2. **Run Unit Tests**:
   ```bash
   mvn test -pl jbh-finance/jbh-finance-application -Dtest=<TestClassName>
   ```

3. **Compile Infrastructure Module**:
   ```bash
   mvn clean compile -pl jbh-finance/jbh-finance-infra -am -DskipTests
   ```

4. **Verify All Tests Pass**: See [Testing Standards](../standards/testing.md) for coverage requirements

### Step 8: Completion Summary

Provide the user with:

1. **Files Created/Modified** - List with locations
2. **API Endpoint** - Full endpoint path and method
3. **Use Case Behavior** - Brief description of what it does
4. **Test Results** - Number of tests, coverage
5. **Next Steps** - Integration testing suggestions

## Important Notes

### Code Quality

See [Code Quality Standards](../standards/code-quality.md) for complete PMD rules and SOLID principles.

**Key Rules:**
- All variables must be final
- No literals in if statements
- No generic exceptions
- Maximum 3 method parameters

### Naming Conventions

See [Naming Conventions](../standards/naming.md) for complete naming patterns.

**Critical Patterns:**
- Use Case Interface: `<Action><Entity>UseCase`
- Input Port: `<Action><Entity>InputPort`
- Command: `<Action><Entity>Command`
- Response: `<Entity>Response` (NOT DTO!)
- Repository: `<Entity><Query|Writer>Repository`

### Architecture Rules

**CRITICAL (from hexagonal architecture standards):**
- **NEVER** put Request/Response in application module
- **NEVER** inject InputPort, always inject UseCase interface
- **NEVER** use @Inject in InputPort constructor
- **ALWAYS** use final for all fields
- **ALWAYS** validate inputs (null checks first)
- **ALWAYS** use fromDTO() for Response objects
- **ALWAYS** document with full JavaDoc

See [Hexagonal Layers](../architecture/hexagonal-layers.md) for complete rules.

## Example Session Flow

1. User runs: `/build-usecase`
2. You ask questions to gather requirements
3. You create all necessary files following templates from `patterns/use-case-complete.md`
4. You run tests and verify compilation
5. You provide completion summary with API endpoint details

## Success Criteria

- ✅ All files created in correct locations
- ✅ Naming conventions followed (see [Naming Standards](../standards/naming.md))
- ✅ JavaDoc documentation complete (see [JavaDoc Standards](../standards/javadoc.md))
- ✅ Unit tests pass (see [Testing Standards](../standards/testing.md))
- ✅ Code compiles successfully
- ✅ CDI configuration updated
- ✅ API endpoint documented
- ✅ PMD rules compliant (see [Code Quality](../standards/code-quality.md))
- ✅ Architecture patterns followed (see [Use Case Pattern](../patterns/use-case-complete.md))

---

**Key References:**
- **Templates:** [Use Case Complete Pattern](../patterns/use-case-complete.md)
- **Naming:** [Naming Conventions](../standards/naming.md)
- **Quality:** [Code Quality Standards](../standards/code-quality.md)
- **Testing:** [Testing Standards](../standards/testing.md)
- **JavaDoc:** [Documentation Standards](../standards/javadoc.md)
