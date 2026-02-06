# Validate API Naming Convention

Analyzes API endpoints, methods, and objects to ensure compliance with the project's naming conventions and generates a remediation plan for violations.

## Arguments

- **Optional:** Path to a specific file, class, or package to validate
- **Optional:** API endpoint pattern (e.g., `/api/v1/monthly-balance`)

If no arguments provided, validates the most recently created/modified API code.

## Instructions

You are a **Backend API Naming Convention Validator**. Your goal is to ensure all backend classes follow the strict naming conventions defined in the project documentation.

### Step 1: Identify Target for Analysis

1. If a file path or class name is provided, analyze that specific file
2. If an API endpoint pattern is provided, find all controllers handling that endpoint
3. If no arguments, search for recently modified controller files in the last commit

### Step 2: Analyze Naming Conventions

For each API class found, verify compliance with these rules:

#### 2.1 Entity Classes (`<EntityName>Entity`)
- ✅ **MUST** end with `Entity` suffix
- ✅ **MUST** be in `*.infrastructure.adapters.output.persistence.entity` package
- ✅ **MUST** contain ORM annotations (@Entity, @Table, etc.)
- ❌ **MUST NOT** be exposed in API controllers or responses
- ❌ **MUST NOT** be returned by service methods

#### 2.2 Internal DTOs (`<EntityName>DTO`)
- ✅ **MUST** end with `DTO` suffix
- ✅ **SHOULD** be in `*.application.core.domain.dto` package
- ✅ Can be used internally between services and repositories
- ❌ **MUST NOT** be serialized to API responses
- ❌ **MUST NOT** be used in controller return types

#### 2.3 Request Objects (`<EntityName>Request`)
- ✅ **MUST** end with `Request` suffix
- ✅ **SHOULD** be in `*.infrastructure.adapters.input.rest.request` package
- ✅ **MUST** be used only as controller method parameters
- ✅ Should contain validation annotations (@NotNull, @Valid, etc.)
- ❌ **MUST NOT** be reused as domain or persistence models

#### 2.4 Response Objects (`<EntityName>Response`)
- ✅ **MUST** end with `Response` or `ApiResponse` suffix
- ✅ **SHOULD** be in `*.infrastructure.adapters.input.rest.response` package
- ✅ **MUST** be used only as controller method return types
- ❌ **MUST NOT** be named `DTO`
- ❌ **MUST NOT** expose Entity classes directly
- ❌ **MUST NOT** be reused as internal DTOs

### Step 3: Validate Method Naming and Consistency

For each API method, verify:

1. **HTTP Method vs. Method Name Alignment:**
   - `POST` → `create*`, `add*`, `register*`
   - `GET` → `get*`, `find*`, `retrieve*`, `list*`
   - `PUT/PATCH` → `update*`, `modify*`, `edit*`
   - `DELETE` → `delete*`, `remove*`

2. **Method Signature Consistency:**
   - Method parameters should use `*Request` objects
   - Return types should use `*Response` objects
   - Entity or DTO should NOT appear in controller signatures

3. **Naming Coherence:**
   - If method handles `MonthlyBalance`, all objects should use `MonthlyBalance` prefix
   - Request: `MonthlyBalanceRequest`
   - Response: `MonthlyBalanceResponse`
   - DTO: `MonthlyBalanceDTO`
   - Entity: `MonthlyBalanceEntity`

### Step 4: Generate Validation Report

Create a detailed report with:

#### ✅ Compliant Items
List all classes/methods that follow conventions correctly.

#### ❌ Violations Found
For each violation, report:
- **File:** Full path to the file
- **Class/Method:** Name of the problematic class or method
- **Issue:** Specific naming convention violated
- **Current Name:** The incorrect name currently in use
- **Suggested Name:** The correct name according to conventions
- **Impact:** What needs to change (e.g., "Used in 3 controller methods")

#### Example Violation Report:
```
❌ VIOLATION: Response object named as DTO
File: jbh-personal-finance-service/src/main/java/.../MonthlyBalanceDTO.java
Class: MonthlyBalanceDTO
Issue: Response object incorrectly named as DTO
Current Name: MonthlyBalanceDTO
Suggested Name: MonthlyBalanceResponse
Impact: Returned by 2 controller methods:
  - MonthlyBalanceController.getMonthlyBalance()
  - MonthlyBalanceController.listMonthlyBalances()
```

### Step 5: Generate Remediation Plan

Create a step-by-step plan to fix all violations:

1. **Phase 1: Rename Classes**
   - List all classes to rename
   - Include full search/replace patterns
   - Note dependencies that will be affected

2. **Phase 2: Update References**
   - List all files that reference the renamed classes
   - Include import statements to update
   - Include method signatures to update

3. **Phase 3: Verification**
   - Run tests to ensure no breakage
   - Verify API contracts remain stable
   - Check Swagger/OpenAPI documentation updates

### Step 6: Ask for Approval

Present the validation report and remediation plan, then ask:

**"Would you like me to:**
1. **Execute the remediation plan** (rename all violations)
2. **Fix specific violations** (you choose which ones)
3. **Export the report only** (no changes)"

### Step 7: Execute (if approved)

If user approves execution:
1. Create a git branch: `fix/api-naming-conventions-{date}`
2. Execute renames using IDE refactoring (preserve git history)
3. Update all references
4. Run tests to verify
5. Generate a commit with detailed change summary
6. Report completion status

## Output Format

Always provide:
1. **Summary Statistics:** Total classes analyzed, violations found, compliance percentage
2. **Detailed Report:** All violations with suggested fixes
3. **Remediation Plan:** Step-by-step fix instructions
4. **Approval Request:** Ask user how to proceed

## Example Usage

```
/validate-api-naming
/validate-api-naming src/main/java/com/jbh/personalfinance/controller/MonthlyBalanceController.java
/validate-api-naming /api/v1/monthly-balance
```

## Notes

- This skill is READ-ONLY by default - it only analyzes and reports
- Changes are only made after explicit user approval
- Always preserve API contract stability - response field names should not change unless required
- When renaming, use IDE refactoring tools to maintain git history
- Consider backwards compatibility for public APIs
