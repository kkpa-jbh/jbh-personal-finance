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

## Current Infrastructure

**Existing Services:**
- `jbh-gateway` - API Gateway (Quarkus-based)
- `jbh-consul-service-discovery` - Service discovery (Consul on port 8500)
- `jbh-iam-service` - User authentication and authorization

**Architecture Model:**
- Single deployable unit with well-defined modules
- PostgreSQL with separate schema per module
- JWT-based authorization (RS256) for all HTTP requests and inter-module communication
- Consul service discovery
- Budget constraint: Cost-effective hosting solutions only (no cloud provider dependencies)
