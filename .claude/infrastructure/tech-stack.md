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

### JWT with RS256

**Authentication:** JSON Web Tokens (JWT)

- RS256 asymmetric signing algorithm
- Public/private key pair
- Token issued by `jbh-iam-service`
- Validated by all services

**Authorization:**

- Role-based access control (RBAC)
- Claims embedded in JWT (user ID, roles, permissions)
- Verified on every HTTP request
- Inter-module communication also JWT-protected

## Service Discovery

### Consul

**Service Registry:** HashiCorp Consul

- Running on port 8500
- Service registration and health checks
- DNS-based service discovery
- Key-value store for configuration

**Integration:**

- Quarkus Consul extension
- Automatic service registration on startup
- Health endpoint integration
- Dynamic configuration updates

## API Gateway

### Quarkus-Based Routing

**Gateway Service:** `jbh-gateway`

- Quarkus-based routing layer
- Single entry point for all clients
- Request routing to downstream services
- JWT validation
- Rate limiting and throttling
- CORS handling

**Features:**

- Reverse proxy to backend modules
- Path-based routing rules
- Circuit breaker for fault tolerance
- Request/response transformation
