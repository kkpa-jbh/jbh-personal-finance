# JBH Personal Finance - Claude Instructions

## Quick Rules

**Development Practices** (details in [code-quality.md](standards/code-quality.md) and [testing.md](standards/testing.md)):
- Generate unit tests for all new code (minimum 50% coverage) except `jbh-z-assembly` and `jbh-*-infra` modules
- Declare all variables `final` when possible
- Avoid using literals in if statements
- Avoid boolean parameters (except in constructors) - use value objects instead
- Maximum 3 method parameters - use command objects or value objects for more
- PMD: CI fails on any violation of `pmd-ruleset.xml` (almost all PMD categories). Check "Common traps" in code-quality.md before you finish.

**Build & verify:** the suite command is `mvn clean verify` (tests + PMD + SpotBugs, like CI).
`mvn test` and `mvn package` skip PMD. One module: `mvn -pl <module-path> -am verify -DskipTests`.

**Horizontal scaling (always):**
- The app runs as more than one instance. Every design and fix must stay correct with 2 or more instances.
- Never protect shared data with in-JVM state: no `synchronized`, in-memory locks, local caches, single-thread executors, or in-memory queues for correctness. They are allowed only to make one instance faster.
- Protect shared data in PostgreSQL: a transaction with a row lock (`SELECT ... FOR UPDATE`), an optimistic `@Version` column, a unique constraint, or an atomic SQL update.
- Every design must say what happens when 2 instances handle the same product at the same time.

## Architecture

This is a Quarkus multi-module monolith using **Hexagonal Architecture** with **feature-based organization**.

**Key Principles:**
- **Dependency Direction**: Application layer NEVER depends on Infrastructure
- **Request/Response Placement**: CRITICAL - Request/Response objects belong in `*-infra` module, NOT application
- **Feature-Based**: Organize by business features (vertical slices), not technical layers
- **Framework Independence**: Domain/application modules have ZERO framework dependencies

## Docs index

| Topic | Doc | Read it when |
|-------|-----|--------------|
| Architecture | [architecture/README.md](architecture/README.md) | Start here for hexagonal principles |
| | [hexagonal-layers.md](architecture/hexagonal-layers.md) | Placing a class in application vs infra |
| | [feature-structure.md](architecture/feature-structure.md) | Creating a feature package |
| | [rest-organization.md](architecture/rest-organization.md) | Adding a REST endpoint |
| | [dependency-rules.md](architecture/dependency-rules.md) | Touching JPMS or Jandex config |
| Standards | [code-quality.md](standards/code-quality.md) | PMD rules and SOLID |
| | [naming.md](standards/naming.md) | Naming an Entity, DTO, Request, Response, UseCase, InputPort |
| | [testing.md](standards/testing.md) | Writing unit tests |
| | [business-rules.md](standards/business-rules.md) | Placing a validation or business rule |
| | [javadoc.md](standards/javadoc.md) | Documenting a use case interface |
| Patterns | [use-case-complete.md](patterns/use-case-complete.md) | Building a full use case (all layers) |
| | [mapping-flow.md](patterns/mapping-flow.md) | Mapping Entity → DTO → Response |
| Infrastructure | [database.md](infrastructure/database.md) | Schemas, primary keys, migrations |
| | [deployment.md](infrastructure/deployment.md) | Port, instances, hosting, monitoring |
| | [tech-stack.md](infrastructure/tech-stack.md) | Quarkus, PostgreSQL, JWT, Consul, gateway |

How this service connects to the other JBH services: `../jbh-deploy/README.md`.

## Agents & Skills

- Agent: [Quarkus Multimodule Architect](agents/quarkus-multimodule-architect.md) - Architecture guidance
- `/jbh-build-usecase` - Generate a complete use case implementation
- `/jbh-validate-api-convention` - Validate API naming conventions
- `/jbh-fe-api-instructions` - Generate frontend instructions from backend API specs
- `/jbh-generate-tests` - Generate unit tests for domain/application classes
- `/jbh-review-business-rules` - Check that business rules sit in the correct hexagonal layer

## System map sync (`../jbh-deploy/README.md`)

This repo is one part of the JBH system. The map of how all JBH services talk to each other
lives in `../jbh-deploy/README.md` (the `jbh-deploy` repo, a sibling of this one in the `kkpa-jbh` folder).

**Rule:** when a change in this repo affects how services communicate, update `../jbh-deploy/README.md`
in the same task. Change only the rows or lines that are affected. Then commit it in the
`jbh-deploy` repo (a separate commit there; never one commit across repos).

Triggers for this repo:
- Add or rename a top-level path under `/jbh-api` (it may need a new gateway route)
- Change the port (`quarkus.http.port`), the Consul name (`consul.service.name`), or the DB
- Add or remove a call through `jbh-gateway-client` (for example, `findUserId`)
- Change `jbh-notification-contracts` (used by `jbh-gateway-client` and `jbh-iam`)
- Add a new module that exposes REST endpoints

If you are not sure a change counts, read `../jbh-deploy/README.md` and check whether any line is now wrong.

## Pipeline config (kkpa-orchestrate-change)

- **Modules-Root:** `.` (the modules are `jbh-finance`, `jbh-notification`, `jbh-preferences`, `jbh-products`, `jbh-commons-lib`)
- **Doc-Pack:** `.claude/CLAUDE.md`, `.claude/standards/code-quality.md`, `.claude/standards/testing.md`, `.claude/standards/naming.md`, `.claude/standards/business-rules.md`, `.claude/architecture/hexagonal-layers.md`
- **Design-Gates:** `database`
- **Git-Strategy:** `auto`
