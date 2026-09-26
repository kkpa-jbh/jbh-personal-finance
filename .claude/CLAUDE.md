# JBH Personal Finance - Claude Instructions

## Quick Rules

**Development Practices:**
- Always use Context7 MCP when you need library/API documentation, code generation, setup, or configuration steps
- Generate unit tests for all new code (minimum 50% coverage) except `jbh-z-assembly` and `jbh-*-infra` modules
- Declare all variables `final` when possible
- Avoid using literals in if statements
- Avoid boolean parameters (except in constructors) - use value objects instead
- Maximum 3 method parameters - use command objects or value objects for more

**Code Quality:**
- See [Code Quality Standards](standards/code-quality.md) for PMD rules and SOLID principles
- See [Naming Conventions](standards/naming.md) for comprehensive naming standards
- See [Testing Requirements](standards/testing.md) for test patterns and coverage rules
- See [Business Rules Distribution](standards/business-rules.md) for where to place validations, constraints, and business logic across domain/application/infra layers

**Documentation:**
- See [JavaDoc Standards](standards/javadoc.md) for use case documentation templates

## Architecture

This is a Quarkus multi-module monolith using **Hexagonal Architecture** with **feature-based organization**.

**Core Concepts:**
- [Architecture Overview](architecture/README.md) - Start here for hexagonal architecture principles
- [Hexagonal Layers](architecture/hexagonal-layers.md) - Application vs Infrastructure module responsibilities
- [Feature Structure](architecture/feature-structure.md) - Feature-based package organization
- [REST Organization](architecture/rest-organization.md) - Feature-based REST API structure
- [Dependency Rules](architecture/dependency-rules.md) - JPMS and Jandex configuration

**Key Principles:**
- **Dependency Direction**: Application layer NEVER depends on Infrastructure
- **Request/Response Placement**: CRITICAL - Request/Response objects belong in `*-infra` module, NOT application
- **Feature-Based**: Organize by business features (vertical slices), not technical layers
- **Framework Independence**: Domain/application modules have ZERO framework dependencies

## Standards

**Naming Conventions:**
- [Complete Naming Guide](standards/naming.md) - Entity, DTO, Request, Response, UseCase, InputPort naming patterns

**Code Quality:**
- [PMD Rules & SOLID Principles](standards/code-quality.md) - Code quality standards and best practices

**Documentation:**
- [JavaDoc Templates](standards/javadoc.md) - Use case interface documentation (class & method level)

**Testing:**
- [Testing Standards](standards/testing.md) - Coverage requirements, patterns, and best practices

**Business Rules:**
- [Business Rules Distribution](standards/business-rules.md) - Where to place validations and rules across domain, application, and infrastructure layers

## Patterns

**Use Case Implementation:**
- [Complete Use Case Pattern](patterns/use-case-complete.md) - Full 12-layer use case example with all components

**Data Flow:**
- [Mapping Flow](patterns/mapping-flow.md) - Entity → DTO → Response transformation pattern

## Infrastructure

**Database:**
- [Database Guidelines](infrastructure/database.md) - PostgreSQL, schema-per-module, natural primary keys

**Deployment:**
- [Deployment Strategy](infrastructure/deployment.md) - Single unit deployment, monitoring, tracing

**Technology Stack:**
- [Tech Stack](infrastructure/tech-stack.md) - Quarkus, Maven, PostgreSQL, JWT, Consul

## Agents & Skills

**Agents:**
- [Quarkus Multimodule Architect](agents/quarkus-multimodule-architect.md) - Architecture guidance agent

**Available Skills:**
- `/build-usecase` - Generate complete use case implementation
- `/validate-api-naming` - Validate API naming conventions
- `/fe-api-instructions` - Generate frontend instructions from backend API specs
- `/jbh-generate-tests` - Generate unit tests for domain/application classes following project test standards
- `/jbh-review-business-rules` - Review a Maven module and verify business rules are placed in the correct hexagonal layer

## Current Infrastructure

**Existing Services** (sibling repos; full map in `../README.md`):
- `jbh-gateway` - API Gateway (Kotlin, Spring Cloud Gateway, port 8080). Routes `/jbh-api/finance|products|preferences|notifications/**` here.
- `jbh-discovery-nexus` - Service discovery (Consul in Docker, port 8500). This app registers as `jbh-personal-finance`.
- `jbh-iam` - User authentication and authorization (Consul name `jbh-iam-service`, port 9999)
- `jbh-gateway-client` - Java library used by the `*-infra` modules to call other services through the gateway

**Architecture Model:**
- Single deployable unit with well-defined modules (port 7777)
- PostgreSQL with separate schema per module (database `jbh_finance`)
- JWT (HMAC shared secret) is issued by `jbh-iam` and validated by `jbh-gateway`. This app does not validate the JWT itself: `BaseRestAdapter.findUserId(...)` asks `jbh-iam` for the user id through `jbh-gateway-client`.
- `jbh-notification-contracts` is also used by `jbh-gateway-client` and `jbh-iam`. A change there needs `mvn install` before those repos build.
- Consul service discovery
- Budget constraint: Cost-effective hosting solutions only (no cloud provider dependencies)

## System map sync (`../README.md`)

This repo is one part of the JBH system. The map of how all JBH services talk to each other
lives in `../README.md` (relative to this repo's root: the `kkpa-jbh` folder that holds all JBH repos).

**Rule:** when a change in this repo affects how services communicate, update `../README.md`
in the same task. Change only the rows or lines that are affected. Then tell the user that
`../README.md` changed, because that folder is not a git repo and the change is not versioned.

Triggers for this repo:
- Add or rename a top-level path under `/jbh-api` (it may need a new gateway route)
- Change the port (`quarkus.http.port`), the Consul name (`consul.service.name`), or the DB
- Add or remove a call through `jbh-gateway-client` (for example, `findUserId`)
- Change `jbh-notification-contracts` (used by `jbh-gateway-client` and `jbh-iam`)
- Add a new module that exposes REST endpoints

If you are not sure a change counts, read `../README.md` and check whether any line is now wrong.
