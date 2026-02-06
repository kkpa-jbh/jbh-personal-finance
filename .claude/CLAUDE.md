# General Instructions

- Always use Context7 MCP when I need library/API documentation, code generation, setup or configuration steps without me having to explicitly ask.
- Always generate unit tests for new code generated on any module different from jbh-z-assembly and jbh-*****-infra. The minimum coverage is 50%.

## PMD Rules

- Declare all variabels final as possible
- Avoid using literals in if statements.
- A method/constructor should not explicitly throw java.lang.Exception.
- Avoid catching generic exceptions such as NullPointerException, RuntimeException, Exception in try-catch block.

## Use Case Documentation Standard

All Use Case **interfaces** (in `*.application.core.usecases` package) MUST be documented with comprehensive JavaDoc following this structure:

### Class-Level Documentation

```java
/**
 * [Technical purpose - what this use case does]
 *
 * <p><strong>User Explanation:</strong> "[User-friendly explanation for FE display -
 * describe the action in simple terms as if explaining to end user]"
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>[Key business rule 1]
 *   <li>[Key business rule 2]
 *   <li>[Additional rules as needed]
 * </ul>
 */
```

### Method-Level Documentation

```java
/**
 * [Brief description of what the method does]
 *
 * <p><strong>Validations:</strong>
 *
 * <ul>
 *   <li>[Validation 1 - e.g., "Command cannot be null"]
 *   <li>[Validation 2 - e.g., "User must own the product"]
 *   <li>[Additional validations]
 * </ul>
 *
 * <p><strong>Database Operations:</strong>
 *
 * <ul>
 *   <li>INSERT: [Tables/entities created - e.g., "New movement record"]
 *   <li>UPDATE: [Tables/entities updated - e.g., "Product balance and net flow"]
 *   <li>DELETE: [Tables/entities deleted - e.g., "Sets deleted_at timestamp"]
 * </ul>
 *
 * @param [param] [description]
 * @return [description]
 * @throws BusinessException [when/why exception is thrown]
 */
```

### Important Notes

- Document ONLY the use case interface, NOT the implementation (InputPort classes)
- User Explanation should be written as if speaking directly to the end user
- Database Operations should list ALL database changes (INSERT/UPDATE/DELETE)
- For read-only queries, use "SELECT: [what is queried]" or "None (reads from registry)"
- Validations should include ALL checks performed by the use case

## Architecture Design

- [Architecture](agents/quarkus-multimodule-architect.md) - Quarkus Multimodule Architect
- [Code Design](docs/code-best-practices.md) - Code Best Practices

### Database Schema Guidelines

- **One schema per module**: Each module should use a single PostgreSQL schema
- **No multiple schemas within a module**: Avoid creating multiple schemas (e.g., userprefs + teamprefs) within a single module
- **Use natural primary keys**: When a natural unique identifier exists (user_id, team_id), use it as the primary key instead of creating a separate UUID id column

### Current Infrastructure

- **Existing Services:** `jbh-gateway`, `jbh-consul-service-discovery`, `jbh-iam-service` (user authentication)
- **Target Architecture:** Single deployable unit with well-defined modules
- **Database:** PostgreSQL with separate schema per module
- **Service Discovery:** Consul (running on port 8500)
- **Authentication:** JWT-based authorization for all HTTP requests and inter-module communication

### Deployment

- **Budget Constraint:** Cost-effective hosting solutions only (no cloud provider dependencies)
- **Deployment Model:** Single unit deployment with multiple modules
- **Monitoring:** Centralized logging and monitoring with ELK stack and Prometheus/Grafana
- **Tracing:** Distributed tracing capability with Jaeger or Zipkin

### Technology Stack

- **Runtime:** Quarkus with GraalVM support for native compilation
- **Build Tool:** Maven with multi-module configuration
- **Database:** PostgreSQL with schema-per-module approach
- **Security:** JWT with RS256 signing
- **Service Discovery:** Consul integration
- **API Gateway:** Quarkus-based routing