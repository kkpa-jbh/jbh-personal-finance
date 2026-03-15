---
name: jbh-review-business-rules
description: "Reviews a Maven module (domain, application, or infra) and verifies that business rules are placed in the correct hexagonal layer according to project standards."
---

# Review Business Rules Skill

## Description

Analyzes a specific Maven module to verify that business rules, validations, and constraints are placed in the correct hexagonal layer (domain, application, or infrastructure), following the project's [Business Rules Distribution Standards](../../standards/business-rules.md).

This skill:

- Identifies misplaced business rules (e.g., domain logic in use cases, application logic in adapters)
- Explains why each finding is a violation, referencing the decision rule
- Creates tasks to track the work and executes them in order
- **NEVER modifies test classes** — test changes are reported separately and require explicit confirmation before being the last task executed

## Usage

```
/jbh-review-business-rules
/jbh-review-business-rules jbh-finance/jbh-finance-application
/jbh-review-business-rules jbh-products
```

## Critical Rule: Test Classes Are Untouchable

> **MUST NOT modify any file under `src/test/`** at any point during execution.
>
> Test packages are treated as read-only. If a production code refactoring causes test classes to require changes (e.g., constructor signatures, moved classes, renamed methods), those changes are **collected into a separate test impact report** and presented to the user **after all production fixes are complete**. Test changes are **never executed automatically** — they always require explicit user confirmation.

---

## Instructions

You are a **Business Rules Placement Reviewer** for a Quarkus hexagonal architecture project. Your goal is to ensure business rules, validations, and constraints are placed in the correct module layer according to the project's standards.

**Reference:** All rules are defined in [Business Rules Distribution](../../standards/business-rules.md)

---

### Step 1: Read the Business Rules Standard

Before any analysis, read:

```
/.claude/standards/business-rules.md
```

This is the authoritative source. Keep it in context throughout the review.

---

### Step 2: Identify the Target Module

If the user provided a module path as an argument, use it directly.

If no argument was provided, use the AskUserQuestion tool:

- **Header:** "Module to Review"
- **Question:** "Which Maven module would you like me to review for business rules compliance?"
- **Options** (present as a list the user can select or type their own):
  - `jbh-finance/jbh-finance-domain` — Domain layer (entities, value objects, domain services)
  - `jbh-finance/jbh-finance-application` — Application layer (use cases, commands, ports)
  - `jbh-finance/jbh-finance-infra` — Infrastructure layer (adapters, REST, persistence)
  - `jbh-products/jbh-products-domain`
  - `jbh-products/jbh-products-application`
  - `jbh-products/jbh-products-infra`
  - `jbh-preferences/jbh-preferences-domain`
  - `jbh-preferences/jbh-preferences-application`
  - `jbh-preferences/jbh-preferences-infra`
  - `jbh-notification/jbh-notification-domain`
  - `jbh-notification/jbh-notification-application`
  - `jbh-notification/jbh-notification-infra`
  - Other (user types the path)

Then ask a second question:

- **Header:** "Review Scope"
- **Question:** "Should I review only the selected module, or all three layers of the parent module (domain + application + infra)?"
- **Options:**
  - **Selected module only** — Focus on the single module provided
  - **Full parent module** — Review all three layers (domain, application, infra) of the parent

---

### Step 3: Discover and Read the Code

Use Glob and Read to explore the module's **production** source files only:

1. Find all Java source files: `<module>/src/main/java/**/*.java`
2. **DO NOT read or include** anything under `src/test/` at this stage — test files are analyzed separately and only for impact assessment
3. Read each file, focusing on:
   - **Domain modules:** Entity classes, value objects, domain services, exceptions
   - **Application modules:** Use case implementations (InputPort classes), command objects, output port interfaces
   - **Infrastructure modules:** REST adapters, JPA entities, repository adapters, CDI configuration

Do NOT skip production files — a complete picture is required for accurate analysis.

---

### Step 4: Analyze for Business Rule Placement Violations

Apply the decision rule from [Business Rules Distribution](../../standards/business-rules.md) to every rule, validation, or constraint found.

#### Decision Rule

> "Would a domain expert recognize this rule as part of the business, with no knowledge of the system?"
> - **Yes** → Must be in Domain module
> - **Yes, but requires loading multiple aggregates or coordinating steps** → Must be in Application module
> - **No, purely technical** → Must be in Infrastructure module

#### What to look for by layer:

**In a Domain module — flag if you find:**
- Missing null checks or invariant guards in constructors or factory methods (allows invalid state)
- Business validation deferred to a use case instead of enforced in the VO/entity
- Domain service with infrastructure imports (`jakarta.*`, `io.quarkus.*`)

**In an Application module — flag if you find:**
- Business format validation that belongs in a value object (e.g., IBAN format check)
- Persistence or database logic (SQL, JPA annotations, `EntityManager`)
- Retry logic or circuit breaker thresholds
- Serialization/deserialization logic
- Business invariants that do not require loading aggregates (belong in domain)

**In an Infrastructure module — flag if you find:**
- Business rule logic in a REST adapter or JPA entity beyond persistence constraints
- Authorization logic that should be in a use case
- Domain exception creation with business reasoning
- Business orchestration (sequence of use case steps)

---

### Step 5: Generate the Analysis Report

Present the findings in this format:

#### Summary

| Metric | Value |
|--------|-------|
| Module(s) reviewed | `<path>` |
| Production files analyzed | N |
| Violations found | N |
| Compliance score | N% |

#### ✅ Compliant Items

List rules/validations that are correctly placed, with a brief explanation.

#### ❌ Violations Found

For each violation:

```
❌ VIOLATION: <Short description>
File:        <full path>
Class:       <ClassName>
Method/Line: <methodName()> or line N
Layer:       <where it currently is>
Should be:   <where it belongs>
Rule:        <which decision rule applies — reference business-rules.md>
Reason:      <explain why this is misplaced in plain language>
Suggested fix: <concrete recommendation>
Test impact: <YES/NO — if YES, describe which test classes will need updating>
```

#### ⚠️ Warnings (Potential Issues)

Ambiguous cases that may or may not be violations depending on intent. Describe the concern and ask for clarification if needed.

---

### Step 6: Generate Remediation Plan & Create Tasks

Create a prioritized remediation plan and register each item as a task using TaskCreate.

**Task creation order (strictly follow this sequence):**

**Priority 1 — Critical (domain invariants missing or broken):**
- Rules that allow entities/VOs to exist in invalid state

**Priority 2 — High (wrong layer, business logic leaking into infra or vice versa):**
- Business logic in adapters, REST controllers, or JPA entities

**Priority 3 — Medium (application layer holding domain-only rules):**
- Format validations or entity constraints duplicated in use cases

**Last Task — Test Impact Report (always last, always requires confirmation):**
- Created only if any production fix causes test classes to require changes
- Title: `[TEST IMPACT] Review and update affected test classes`
- This task is NEVER executed automatically — see Step 9

For each production fix task, include:
1. What to create/move (class or method)
2. Target location
3. What to remove from the current location
4. Whether test classes are impacted (flag only — do not include test changes in this task)

---

### Step 7: Ask for Approval

Present the full report and the task list, then ask:

**"How would you like to proceed?"**

1. **Execute full remediation plan** — Work through all tasks in order (tests task excluded)
2. **Fix specific violations** — You select which tasks to execute
3. **Report only** — No changes, just the analysis
4. **Discuss a specific finding** — Explain or debate a particular case

---

### Step 8: Execute Production Fixes (if approved)

Work through each task in priority order using TaskUpdate to mark tasks `in_progress` → `completed`:

For each production fix:

1. Mark task as `in_progress`
2. Make the changes to **production files only** (`src/main/java/` — NEVER `src/test/`)
3. Preserve all existing behavior — only move/restructure, do not change logic
4. Update any references in production code (imports, CDI configuration)
5. Compile the affected module to verify:
   ```bash
   mvn clean compile -pl <module-path> -am -DskipTests
   ```
6. Mark task as `completed`
7. Move to the next task

If compilation fails, stop and report the error before continuing.

---

### Step 9: Test Impact Report (last task — requires explicit confirmation)

After all production tasks are completed, if there is a test impact task:

1. Mark the test impact task as `in_progress`
2. Read the affected test files under `src/test/` (read-only at this point)
3. Generate the **Test Impact Report**:

```
📋 TEST IMPACT REPORT
─────────────────────────────────────────────────
The following test classes require updates due to production code changes.
NO changes have been made yet. Explicit confirmation is required.

For each affected test file:

  File:    <full path to test class>
  Reason:  <what changed in production that affects this test>
  Changes needed:
    - <specific line or method that needs updating>
    - <suggested new code or approach>

─────────────────────────────────────────────────
⚠️  MUST NOT proceed without your confirmation.

Do you want me to apply these test changes?
  1. Yes — apply all test changes listed above
  2. Yes, but only specific ones — tell me which files
  3. No — I will handle test updates manually
```

4. Wait for explicit user confirmation before touching any test file
5. If confirmed, apply only the approved test changes, then mark the task as `completed`
6. Run tests to verify:
   ```bash
   mvn test -pl <module-path>
   ```

---

## Output Format

Always provide:

1. **Summary table** — production files analyzed, violations found, compliance score
2. **Detailed report** — all violations with file paths, reasons, test impact flag, and suggested fixes referencing [business-rules.md](../../standards/business-rules.md)
3. **Task list** — one task per violation (production fixes first, test impact last)
4. **Approval request** — ask before executing anything

## Notes

- **MUST NOT touch `src/test/`** at any point during production fix execution
- This skill is **READ-ONLY by default** — analysis only until the user explicitly approves
- Test impact is always the **last task** and always requires **separate explicit confirmation**
- Always reference [Business Rules Distribution](../../standards/business-rules.md) in violation explanations
- When in doubt about a finding, flag it as a warning and explain the ambiguity
- Respect [Code Quality Standards](../../standards/code-quality.md) when suggesting fixes
- Fixes must maintain compliance with [Naming Conventions](../../standards/naming.md)
