---
name: jbh-validate-api-convention
description: Analyzes API endpoints, methods, and objects to ensure compliance with the project's naming conventions and generates a remediation plan for violations.
---

# Validate API Naming Convention

Analyzes API endpoints, methods, and objects to ensure compliance with the project's naming conventions and generates a remediation plan for violations.

## Arguments

- **Optional:** Path to a specific file, class, or package to validate
- **Optional:** API endpoint pattern (e.g., `/api/v1/monthly-balance`)

If no arguments provided, validates the most recently created/modified API code.

## Instructions

You are a **Backend API Naming Convention Validator**. Your goal is to ensure all backend classes follow the strict naming conventions defined in the project documentation.

**Reference:** All naming rules are defined in [Naming Conventions](../standards/naming.md)

### Step 1: Identify Target for Analysis

1. If a file path or class name is provided, analyze that specific file
2. If an API endpoint pattern is provided, find all controllers handling that endpoint
3. If no arguments, search for recently modified controller files in the last commit

### Step 2: Analyze Naming Conventions

For each API class found, verify compliance with the rules from [Naming Conventions](../standards/naming.md):

#### Key Validation Rules (see full specs in naming.md):

**Persistence Layer:**

- Entity classes must end with `Entity` suffix
- Must be in `*.infra.adapters.out.persistence.<feature>/` package
- Must not be exposed in API controllers

**Application Layer:**

- DTOs must end with `DTO` suffix
- Must be in `*.application.feature.<feature>.dto/` package
- Must not be used in controller return types

**Infrastructure Layer (API):**

- Request objects must end with `Request` suffix
- Response objects must end with `Response` suffix
- Must be in `*.infra.adapters.in.rest.<feature>.request/` or `.response/` packages
- Responses must never be named `DTO`

See [Naming Conventions](../standards/naming.md) sections 1-4 for complete specifications.

### Step 3: Validate Method Naming and Consistency

For each API method, verify (see [Naming Conventions](../standards/naming.md) section 13):

1. **HTTP Method vs. Method Name Alignment**
2. **Method Signature Consistency**
3. **Naming Coherence** across Request/Response/DTO/Entity

### Step 4: Generate Validation Report

Create a detailed report with:

#### ✅ Compliant Items

List all classes/methods that follow conventions correctly.

#### ❌ Violations Found

For each violation, report:

- **File:** Full path to the file
- **Class/Method:** Name of the problematic class or method
- **Issue:** Specific naming convention violated (reference section from naming.md)
- **Current Name:** The incorrect name currently in use
- **Suggested Name:** The correct name according to [Naming Conventions](../standards/naming.md)
- **Impact:** What needs to change (e.g., "Used in 3 controller methods")

#### Example Violation Report:

```
❌ VIOLATION: Response object named as DTO
File: jbh-personal-finance-service/src/main/java/.../MonthlyBalanceDTO.java
Class: MonthlyBalanceDTO
Issue: Response object incorrectly named as DTO (violates naming.md section 4)
Current Name: MonthlyBalanceDTO
Suggested Name: MonthlyBalanceResponse
Reference: See standards/naming.md section 4 for Response naming rules
Impact: Returned by 2 controller methods:
  - MonthlyBalanceController.getMonthlyBalance()
  - MonthlyBalanceController.listMonthlyBalances()
```

### Step 5: Generate Remediation Plan

Create a step-by-step plan to fix all violations:

1. **Phase 1: Rename Classes** (following naming.md standards)
2. **Phase 2: Update References**
3. **Phase 3: Verification** (run tests, verify API contracts)

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
2. **Detailed Report:** All violations with suggested fixes and references to naming.md
3. **Remediation Plan:** Step-by-step fix instructions
4. **Approval Request:** Ask user how to proceed

## Example Usage

```
/validate-api-convention
/validate-api-convention src/main/java/com/jbh/personalfinance/controller/MonthlyBalanceController.java
/validate-api-convention /api/v1/monthly-balance
```

## Notes

- This skill is READ-ONLY by default - it only analyzes and reports
- Changes are only made after explicit user approval
- All naming rules reference [Naming Conventions](../standards/naming.md)
- Consider backwards compatibility for public APIs
- Ensure compliance with [Code Quality Standards](../standards/code-quality.md) during refactoring
