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

First, read the architecture patterns file:

```
/.claude/docs/use-case-architecture-patterns.md
```

This file contains all the patterns, naming conventions, and templates you need.

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

For each file, use the templates from `use-case-architecture-patterns.md`:

#### 4.1 Use Case Interface

- **Location**: `jbh-finance-application/src/main/java/com/jbh/finance/application/feature/<feature>/usecases/`
- **Naming**: `<Action><Entity>UseCase.java`
- **Template**: Follow the JavaDoc template with:
    - Technical purpose
    - User Explanation (user-friendly)
    - Business Rules
    - Method documentation (Validations, Database Operations)

#### 4.2 Input Port Implementation

- **Location**: `jbh-finance-application/src/main/java/com/jbh/finance/application/feature/<feature>/ports/input/`
- **Naming**: `<Action><Entity>InputPort.java`
- **Pattern**:
    - Constructor injection (NO @Inject annotation)
    - All fields final
    - Null validations first
    - Command validation (if applicable)
    - Business logic via services
    - Return DTOs

#### 4.3 Command Object (if needed)

- **Location**: `jbh-finance-application/src/main/java/com/jbh/finance/application/feature/<feature>/commands/`
- **Naming**: `<Action><Entity>Command.java`
- **Pattern**:
    - Record with custom builder
    - `validate()` method
    - JavaDoc with usage examples

#### 4.4 Output Port (if needed)

- **Location**: `jbh-finance-application/src/main/java/com/jbh/finance/application/feature/<feature>/ports/output/`
- **Naming**: `<Entity>QueryRepository` or `<Entity>WriterRepository`
- **Pattern**: Interface with method signatures only

#### 4.5 Unit Tests

- **Location**: `jbh-finance-application/src/test/java/com/jbh/finance/application/core/usecases/mock/`
- **Naming**: `<Action><Entity>MockTest.java`
- **Pattern**: Follow FindMovementsByProductMockTest pattern
- **Tests to include**:
    - Happy path
    - Validation errors (null checks)
    - Business exceptions
    - Edge cases
    - Sorting/ordering (if applicable)

### Step 5: Build Infrastructure Layer

#### 5.1 Repository Adapter (if needed)

- **Location**: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/out/persistence/<feature>/`
- **Naming**: `<Entity>RepositoryQueryAdapter.java` or `<Entity>RepositoryWriterAdapter.java`
- **Pattern**: @ApplicationScoped, inject JPA repository, implement interface

#### 5.2 JPA Repository (if needed)

- **Location**: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/out/persistence/<feature>/`
- **Naming**: `<Entity>JPARepository.java`
- **Pattern**:
    - PanacheRepository
    - @PersistenceUnit(name = "finance")
    - Use Parameters.with() for queries
    - Map to DTO via entity.toDTO()

#### 5.3 REST Adapter

- **Location**: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/rest/<feature>/`
- **Naming**: `<Feature>RestAdapter.java` (may already exist)
- **Pattern**:
    - @RequestScoped
    - @Path from FinanceApiRoutes
    - Inject UseCase interface (not InputPort!)
    - GET for queries, POST for commands
    - @SecurityRequirement(name = "JWT")
    - findUserId(authHeader) for user extraction
    - Transform DTO → Response

#### 5.4 Request Object (for Commands)

- **Location**: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/rest/<feature>/request/`
- **Naming**: `<Action><Entity>Request.java`
- **Pattern**: Record with fields matching API contract

#### 5.5 Response Object

- **Location**: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/adapters/in/rest/<feature>/response/`
- **Naming**: `<Entity>Response.java` (NEVER `<Entity>DTO`!)
- **Pattern**:
    - Record matching DTO fields
    - Static `fromDTO(DTO dto)` factory method

#### 5.6 CDI Configuration

- **Location**: `jbh-finance-infra/src/main/java/com/jbh/finance/infra/ProductUseCasesCDIConfig.java`
- **Pattern**:
    - Add import for InputPort and UseCase
    - Add @Produces @ApplicationScoped method
    - Return InputPort instance as UseCase interface type
    - Wire dependencies via constructor

### Step 6: Implementation Order

**CRITICAL**: Implement files in this order:

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

4. **Verify All Tests Pass**:
    - All unit tests must pass
    - Code must compile without errors
    - Minimum 50% coverage achieved

### Step 8: Completion Summary

Provide the user with:

1. **Files Created/Modified** - List with locations
2. **API Endpoint** - Full endpoint path and method
3. **Use Case Behavior** - Brief description of what it does
4. **Test Results** - Number of tests, coverage
5. **Next Steps** - Integration testing suggestions

## Important Notes

### PMD Compliance

- All variables must be final
- No literals in if statements
- No generic exceptions (use BusinessException with BusinessApplicationExceptionType)
- Methods should not throw Exception

### Naming Conventions

Always follow these exact patterns:

- Use Case Interface: `<Action><Entity>UseCase`
- Input Port: `<Action><Entity>InputPort`
- Command: `<Action><Entity>Command`
- Response: `<Entity>Response` (NOT DTO!)
- Repository: `<Entity><Query|Writer>Repository`

### Architecture Rules

- **NEVER** put Request/Response in application module
- **NEVER** inject InputPort, always inject UseCase interface
- **NEVER** use @Inject in InputPort constructor
- **ALWAYS** use final for all fields
- **ALWAYS** validate inputs (null checks first)
- **ALWAYS** use fromDTO() for Response objects
- **ALWAYS** document with full JavaDoc

### Feature Package Structure

```
com.jbh.finance.application.feature.<feature>/
├── dto/                    # DTOs only
├── commands/               # Command objects
├── ports/
│   ├── input/             # InputPort implementations
│   └── output/            # Repository interfaces
├── usecases/              # UseCase interfaces
├── services/              # Domain services
└── validation/            # Validators
```

### Test Coverage

- Minimum 50% coverage required
- Test all validation paths
- Test business logic branches
- Test error cases
- Mock all dependencies

## Example Session Flow

1. User runs: `/build-usecase`
2. You ask questions to gather requirements
3. You create all necessary files following templates
4. You run tests and verify compilation
5. You provide completion summary with API endpoint details

## Success Criteria

- ✅ All files created in correct locations
- ✅ Naming conventions followed exactly
- ✅ JavaDoc documentation complete
- ✅ Unit tests pass (9+ tests typical)
- ✅ Code compiles successfully
- ✅ CDI configuration updated
- ✅ API endpoint documented
- ✅ PMD rules compliant
- ✅ Architecture patterns followed

---

**Remember**: Always reference `~/.claude/use-case-architecture-patterns.md` for templates and patterns. Follow the exact structure shown in the FindMovementsByProduct use case as
the gold standard example.
