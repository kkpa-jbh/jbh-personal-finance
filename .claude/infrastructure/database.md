# Database Guidelines

## PostgreSQL Configuration

### Schema-per-Module Approach

Each module in the system uses a dedicated PostgreSQL schema to maintain clear separation of concerns and ownership boundaries.

**Core Principles:**

- **One schema per module**: Each module (e.g., `jbh-finance-service`, `jbh-iam-service`) operates within its own PostgreSQL schema
- **No multiple schemas within a module**: Avoid creating multiple schemas (e.g., `userprefs` + `teamprefs`) within a single module
- **Schema isolation**: Modules should not cross-query other module schemas directly; use APIs instead

### Primary Key Strategy

**Prefer Natural Primary Keys:**

When a natural unique identifier exists in the business domain, use it as the primary key instead of creating a separate UUID or auto-increment column.

**Examples:**

- `user_id` (from IAM service) as PK instead of separate `id` column
- `team_id` as PK for team-related tables
- `product_id` as PK for financial products

**Benefits:**

- Reduces redundant columns
- Simplifies joins and queries
- Makes relationships more explicit
- Aligns database schema with domain model

**When to use synthetic keys:**

- No obvious natural identifier exists
- Composite keys would be too complex
- Performance considerations (natural key is very long string)

### Database Access Patterns

**Repository Pattern:**

- All database access goes through output ports (repository interfaces) in the application layer
- Infrastructure layer implements these ports using JPA/Hibernate
- Entities are never exposed outside the persistence layer

**Transaction Management:**

- Use the UnitOfWork pattern in the application layer for ACID transactions
- Avoid distributed transactions across modules
- Use eventual consistency with event-driven patterns for cross-module data synchronization
