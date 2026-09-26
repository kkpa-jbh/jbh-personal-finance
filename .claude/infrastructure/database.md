# Database Guidelines

## PostgreSQL Configuration

### Schema-per-Module Approach

Each module in the system uses a dedicated PostgreSQL schema to maintain clear separation of concerns and ownership boundaries.

**Core Principles:**

- **One schema per module**: Each module (e.g., finance, notification, preferences) operates within its own PostgreSQL schema in the `jbh_finance` database. `jbh-iam` is a separate service with its own database config
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

### Migration Files

**Always create a new migration file for every SQL change:**

- Never modify existing migration files that have already been applied
- Name files sequentially: `NNN_descriptive_name.sql` (e.g., `006_add_prepaid_health_category.sql`)
- Place migration files in the corresponding `*-infra` module under `src/main/resources/db/changelog/`
- Register every new migration in the module's Liquibase master XML (e.g., `finance-db-master.xml`)
- If unsure which module a migration belongs to, ask the user before creating the file

### Database Access Patterns

**Repository Pattern:**

- All database access goes through output ports (repository interfaces) in the application layer
- Infrastructure layer implements these ports using JPA/Hibernate
- Entities are never exposed outside the persistence layer

**Transaction Management:**

- Use the UnitOfWork pattern in the application layer for ACID transactions
- Avoid distributed transactions across modules
- Use eventual consistency with event-driven patterns for cross-module data synchronization
