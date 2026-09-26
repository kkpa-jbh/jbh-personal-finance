# Technology Stack

## Core Runtime

### Quarkus with GraalVM

**Runtime Framework:** Quarkus

- Supersonic Subatomic Java
- Native compilation support with GraalVM
- Fast startup time (milliseconds)
- Low memory footprint
- Developer-friendly with live reload

**GraalVM Native Image:**

- Compile to native executable
- Reduced container image size
- Faster boot time for cloud deployments
- Lower memory consumption

## Build and Dependency Management

### Maven Multi-Module

**Build Tool:** Apache Maven

- Multi-module project structure
- Centralized dependency management in parent POM
- Module-specific configurations
- Support for incremental builds

**Module Organization:**

- `jbh-*-domain`: Domain entities and business logic
- `jbh-*-application`: Use cases and application services
- `jbh-*-infra`: Infrastructure adapters (REST, persistence)
- `jbh-z-assembly`: Final assembly module

## Data Persistence

### PostgreSQL

**Database:** PostgreSQL 14+

- Schema-per-module approach
- ACID transactions
- JSONB support for flexible data
- Full-text search capabilities
- Robust query optimizer

**Access Layer:**

- Hibernate ORM with Panache
- JPA entities in infrastructure layer
- Repository pattern for data access

## Security

### JWT (HMAC shared secret)

**Authentication:** JSON Web Tokens (JWT)

- Issued by `jbh-iam` (Consul name `jbh-iam-service`) at `/jbh-api/auth/signin`
- Signed with an HMAC shared secret (env `JWT_SECRET`), the same value in `jbh-iam` and `jbh-gateway`
- Validated by `jbh-gateway` only (signature, expiration, `token_type=ACCESS`, `sub` = user id)
- Access token: 15 minutes. Refresh token: 30 days.

**This app does not validate the JWT itself:**

- REST adapters pass the `Authorization` header to `BaseRestAdapter.findUserId(...)`
- It asks `jbh-iam` for the user id through `jbh-gateway`, with `jbh-gateway-client`
- Calls to other services also go through the gateway, with the same token

## Service Discovery

### Consul

**Service Registry:** HashiCorp Consul

- Running on port 8500
- Service registration and health checks
- DNS-based service discovery
- Key-value store for configuration

**Integration:**

- Custom registration class `jbh-z-assembly/.../config/ConsulServiceRegistration.java` (Consul HTTP API, no Quarkus extension)
- Settings: `consul.*` in `jbh-z-assembly/src/main/resources/application.properties`
- Registers as `jbh-personal-finance`, health check `/q/health`
- Consul runs from the `jbh-discovery-nexus` repo (Docker)

## API Gateway

### Spring Cloud Gateway

**Gateway Service:** `jbh-gateway` (separate repo, Kotlin + Spring Cloud Gateway, port 8080)

- Single entry point for all clients
- Finds services in Consul (`lb://jbh-personal-finance`)
- Routes `/jbh-api/finance/**`, `/jbh-api/preferences/**`, `/jbh-api/notifications/**` (and `/jbh-api/products/**`) to this app
- JWT validation
- Rate limiting (10 requests per second per IP)

**Not configured yet:** circuit breaker (the Resilience4J dependency exists but no route uses it).

Full system map: `../jbh-deploy/README.md` (the `jbh-deploy` repo).

