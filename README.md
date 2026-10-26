# jbh-personal-finance

[![CI Pipeline](https://github.com/kkpa-jbh/jbh-personal-finance/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/kkpa-jbh/jbh-personal-finance/actions/workflows/ci.yml)

Backend for a personal finance app. Users track bank products (savings, credit cards,
investments, CDTs), their movements, transfers and monthly balances. A bank statement import
(PDF, OCR, then an LLM) is in progress.

Quarkus modular monolith (Java 21, PostgreSQL). One runtime (port 7777) holds several business
modules. Each module uses hexagonal architecture: `domain`, `application`, `infra`.
Only the `*-infra` modules expose REST APIs.

It is one service of the JBH system. How it connects to the other services (gateway, IAM, Consul):
`jbh-deploy/README.md` in the [jbh-deploy](https://github.com/kkpa-jbh/jbh-deploy) repo.

## Modules

| Module | Submodules | What it does | Gateway paths |
|--------|-----------|--------------|---------------|
| `jbh-commons-lib` | - | Shared code (`api`, `exception`, `time`, `util`) | - |
| `jbh-finance` | `-domain`, `-application`, `-infra` | Products, movements, transfers, categories, balances, statement import | `/jbh-api/finance/**` |
| `jbh-preferences` | `-domain`, `-application`, `-contracts`, `-infra` | User and team preferences | `/jbh-api/preferences/v1`, `/jbh-api/preferences/team-preferences/v1` |
| `jbh-notification` | `-contracts`, `-infra` | Send notifications (email) | `/jbh-api/notifications/v1` |
| `jbh-z-assembly` | - | Quarkus runner. Depends on all `*-infra` modules and holds `application.properties` | - |

## Run it

You need Java 21, `unzip` (the Maven wrapper uses it), Docker (for the database tests) and PostgreSQL on `localhost:5432`.
The Maven wrapper uses Maven 3.9.12. If you run `mvn` yourself, use 3.9.12 or newer (the Quarkus plugin needs it).

```bash
# 1. Env vars. Optional for a first run: without .env, dev mode uses the password postgres.
#    If you copy it, set DATABASE_PASSWORD to your local PostgreSQL password.
cp .env.example .env

# 2. Database jbh_finance and one schema per module
make create-all-schemas

# 3. Shared libraries. jbh-gateway-client needs the notification contracts first.
./mvnw -pl jbh-notification/jbh-notification-contracts -am install -DskipTests
(cd ../jbh-gateway-client && mvn install -DskipTests)

# 4. Start in dev mode on port 7777
./mvnw compile quarkus:dev
```

Without Consul, start with `CONSUL_ENABLED=false`. Then the gateway cannot find the service, but
you can call it directly on port 7777.

- Health check: `GET http://localhost:7777/q/health`
- OpenAPI (dev mode): `http://localhost:7777/q/swagger-ui`

Liquibase runs the migrations at start. Production runs the Docker image from CI
(`jbh-z-assembly/Dockerfile`), see `jbh-deploy`.

## Test it

```bash
# Unit tests, PMD and SpotBugs (like CI)
./mvnw clean verify

# Also the tests tagged "database". They start PostgreSQL in Docker (Quarkus Dev Services).
./mvnw clean verify -Djbh.test.excludedGroups=none
```

CI runs both groups on every push and pull request. The job summary shows line coverage per module
(JaCoCo). Domain and application modules are around 75–95%.

What the tests cover:

- Domain and use case rules, with in-memory fakes of the repositories.
- Architecture rules with ArchUnit: the domain and application layers never import infra or
  framework code.
- A real row lock on PostgreSQL: two transactions add movements to the same month at the same
  time, and no update is lost (`ProcessMovementServiceDatabaseLockTest`).
- The product API over HTTP: a user gets `404` for the product of another user, and `401`
  without a valid token (`ProductRestHttpTest`).

## How it works

```
jbh-app ──► jbh-gateway (JWT check) ──► jbh-personal-finance :7777
                                          │  REST adapter (infra)
                                          │    └─ asks jbh-iam for the user id, through the gateway
                                          │  use case / input port (application)
                                          │  domain model (domain): balances, rules
                                          │  repository adapter (infra) ──► PostgreSQL jbh_finance
                                          └─ statement import ──► Azure OCR ──► OpenRouter / Gemini
```

- **Request flow.** The REST adapter maps HTTP to a command. The use case loads the domain model,
  applies the rules, and saves it through an output port. Every command carries the user id, and
  product reads filter by it.
- **Monthly balances.** A new movement changes the balance of its month and of the months after it.
  The month of the movement is updated in the request. Then an async task checks that month and
  the months after it, under the same product row lock.
- **Statement import (in progress).** The PDF goes to Azure AI Document Intelligence (OCR). An LLM
  (OpenRouter, or Gemini with `STATEMENT_LLM=gemini`) turns the text into movements. Today the
  endpoint answers `202`, runs in the background and only logs the result. Nothing is saved yet.
- **Errors.** Each module has one exception handler. Business rule errors give `422` with a
  message, auth errors give `401`.

```
jbh-personal-finance/
├── pom.xml                      (root parent: BOM and plugin versions)
├── jbh-commons-lib/
├── jbh-finance/
│   ├── jbh-finance-domain/
│   ├── jbh-finance-application/
│   └── jbh-finance-infra/       (REST, persistence, gateway client, AI adapters)
├── jbh-preferences/
├── jbh-notification/
└── jbh-z-assembly/              (Quarkus runner, application.properties, Dockerfile)
```

`jbh-z-assembly` is named `z` so Maven builds it last. It is the only module with the Quarkus
Maven plugin.

## Decisions and trade-offs

- **Modular monolith instead of microservices.** One deploy and one database are cheap to run on a
  small VPS. Cost: the modules share a runtime. Each module has its own schema and talks to the
  others only through ports, so a module can move to its own service later.
- **Hexagonal layers, checked by ArchUnit.** Domain and application code has no Quarkus
  dependency, so it is tested without starting the app. Cost: more classes per feature (command,
  DTO, request, response, mapper).
- **Database locks instead of in-memory locks.** The app runs as 2 or more instances. A movement
  locks its product row (`SELECT ... FOR UPDATE`), so two instances cannot lose an update.
  Cost: writes to the same product wait for each other.
- **The user id comes from IAM, not from the JWT.** Only IAM and the gateway know the JWT secret.
  Cost: one extra HTTP call per request.
- **One products table with a JSONB metadata column** for all product types (ADR-001 in
  `docs/notes/adr.md`). Aggregations need no JOINs, and a new type needs no migration. Cost: the
  database cannot check the metadata fields.

## Limits and next steps

- The async balance sync runs in memory in one instance. If the instance stops, pending tasks are
  lost, and there is no real dead-letter queue yet (`AsyncTaskExecutorImpl`).
- The net profit formula does not work for credit cards yet (`MonthlyBalanceDomain`).
- `/jbh-api/notifications/**` has no service-to-service credential. Production blocks it at Caddy.
- Quarkus runs on 3.33 LTS (3.33.4.1). Plan the next LTS move before this line goes out of support.
- The statement import does not save movements yet.

## Makefile

The root `Makefile` calls each module's `.mk` file (`Finance.mk`, `Notification.mk`, `Preferences.mk`).
The `product-*` targets are for the **Finance** module.

- `make help` - Shows help for all modules
- `make create-all-schemas` - Creates all module schemas
- `make drop-all-schemas` - Drops all schemas (asks for confirmation)
- `make recreate-all-schemas` - Recreates all schemas
- `make check-all-connections` - Tests all database connections
- `make export-all-schemas` - Exports schemas to `docs/database-schemas`

Module commands (same pattern for `notification-*` and `preferences-*`):
`make product-create-schema`, `make product-drop-schema`, `make product-recreate-schema`,
`make product-check-connection`.

## More docs

| Topic | Doc |
|-------|-----|
| Jandex indexing and adding a module | `docs/guides/jandex-and-modules.md` |
| PMD and SpotBugs | `docs/guides/static-analysis.md` |
| Logging | `docs/guides/logging.md` |
| Maven Enforcer | `docs/guides/maven-enforcer.md` |
| Naming conventions | `docs/guides/naming-conventions.md` |
| API routing | `docs/notes/API_ROUTING.md` |
| Architecture decisions | `docs/notes/adr.md` |
| Rules for AI assistants and contributors | `.claude/CLAUDE.md` |
