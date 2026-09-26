# jbh-personal-finance

Quarkus modular monolith. One runtime (port 7777) holds several business modules.
Each module uses hexagonal architecture: `domain`, `application`, `infra`.
Only the `*-infra` modules expose REST APIs.

See the `README.md` in the parent folder for how this service connects to the rest of JBH.

## Modules

| Module | Submodules | What it does | Gateway paths |
|--------|-----------|--------------|---------------|
| `jbh-commons-lib` | - | Shared code (`api`, `exception`, `time`, `util`) | - |
| `jbh-finance` | `-domain`, `-application`, `-infra` | Products, movements, transfers, categories, balances | `/jbh-api/finance/**` |
| `jbh-preferences` | `-domain`, `-application`, `-contracts`, `-infra` | User and team preferences | `/jbh-api/preferences/v1`, `/jbh-api/preferences/team-preferences/v1` |
| `jbh-notification` | `-contracts`, `-infra` | Send notifications (email) | `/jbh-api/notifications/v1` |
| `jbh-z-assembly` | - | Quarkus runner. Depends on all `*-infra` modules and holds `application.properties` | - |

`jbh-products/` holds only an old `.iml` file. It is not a Maven module.

## Links to other projects

- **Consul**: registers as `jbh-personal-finance` (health check `/q/health`). `jbh-gateway` routes to it with `lb://jbh-personal-finance`.
- **User id**: `jbh-finance-infra` and `jbh-preferences-infra` do not read the JWT. `BaseRestAdapter.findUserId(...)` asks `jbh-iam` through the gateway, with `jbh-gateway-client` (`getUserClient().findUserId`).
- **Gateway URL**: `jbh.gateway.base-url` (env `JBH_GATEWAY_URL`, default `http://localhost:8080`).
- **Contracts used by other repos**: `jbh-notification-contracts` is a dependency of `jbh-gateway-client`. `jbh-iam` sends invitation emails to the notifications module with it. Install it (`mvn install`) before you build `jbh-gateway-client`.
- **Database**: Postgres `jbh_finance`, one schema per module (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`).

## Makefile

The root `Makefile` calls each module's `.mk` file (`Finance.mk`, `Notification.mk`, `Preferences.mk`).
Note: the `product-*` targets are for the **Finance** module.

Global commands:

- `make help` - Shows help for all modules
- `make create-all-schemas` - Creates all module schemas
- `make drop-all-schemas` - Drops all schemas (asks for confirmation)
- `make recreate-all-schemas` - Recreates all schemas
- `make check-all-connections` - Tests all database connections
- `make export-all-schemas` - Exports schemas to `docs/database-schemas`

Module help: `make product-help`, `make notification-help`, `make preferences-help`.

Module commands (same pattern for `notification-*` and `preferences-*`):

- `make product-create-schema`, `make product-drop-schema`, `make product-recreate-schema`, `make product-check-connection`

## How it works

- Root POM: BOM and plugin versions. No Quarkus runtime.
- Business modules: split into hexagonal submodules.
- `jbh-z-assembly`:
    - Holds `application.properties` with `quarkus.http.port=7777`. This applies to the whole app.
    - Is the only module with the Quarkus Maven plugin. It builds the runnable JAR.
    - Depends on `jbh-finance-infra`, `jbh-notification-infra` and `jbh-preferences-infra`.
    - Is named `z` so Maven builds it last.

To move a module to its own service later, move its `*-infra` module into a new Quarkus app.

```
jbh-personal-finance/
├── pom.xml                      (root parent)
├── jbh-commons-lib/
├── jbh-finance/
│   ├── jbh-finance-domain/
│   ├── jbh-finance-application/
│   └── jbh-finance-infra/       (REST, persistence, gateway client)
├── jbh-preferences/
│   ├── jbh-preferences-domain/
│   ├── jbh-preferences-application/
│   ├── jbh-preferences-contracts/
│   └── jbh-preferences-infra/
├── jbh-notification/
│   ├── jbh-notification-contracts/
│   └── jbh-notification-infra/
└── jbh-z-assembly/              (Quarkus runner)
    └── src/main/resources/application.properties
```

## Jandex Indexing Strategy (Hexagonal Architecture)

Quarkus requires **Jandex indexing** for CDI discovery and reflection. In a hexagonal architecture, we use a **two-tier approach** to keep domain/application layers framework-agnostic.

### 1. CDI Bean Discovery (Infrastructure Modules)

**✅ Use Jandex Plugin in `*-infra` Modules:**

```xml
<!-- In each *-infra module pom.xml -->
<plugin>
  <groupId>io.smallrye</groupId>
  <artifactId>jandex-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>make-index</id>
      <goals>
        <goal>jandex</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

**Purpose:** Index CDI beans for runtime discovery:
- REST endpoints (`@Path`, `@GET`)
- Services (`@ApplicationScoped`)
- Repositories, adapters

**Result:**
```
jbh-finance-infra.jar → contains META-INF/jandex.idx ✅
jbh-preferences-infra.jar → contains META-INF/jandex.idx ✅
```

**Why NOT in Assembly/Root?**
- Assembly can only index its own classes
- Cannot retroactively index dependency JARs
- Each JAR needs its own `META-INF/jandex.idx`

---

### 2. DTOs/VOs Indexing (Domain/Application Modules)

**✅ Use `quarkus.index-dependency` in Assembly Configuration:**

```properties
# In jbh-z-assembly/src/main/resources/application.properties
quarkus.index-dependency.products-domain.group-id=com.jbh
quarkus.index-dependency.products-domain.artifact-id=jbh-finance-domain
quarkus.index-dependency.products-application.group-id=com.jbh
quarkus.index-dependency.products-application.artifact-id=jbh-finance-application
# ... repeat for preferences, notification, etc.
```

**Purpose:** Index non-CDI classes for OpenAPI/Swagger and reflection:
- DTOs (Data Transfer Objects)
- VOs (Value Objects)
- Domain entities
- Command objects

**Result:** Domain/application modules stay **framework-agnostic** (NO Quarkus dependencies) ✅

**❌ DON'T use Jandex plugin in domain/application modules:**
- Violates hexagonal architecture
- Couples business logic to Quarkus
- Makes it harder to switch frameworks (e.g., Quarkus → Spring)

---

### Comparison: Plugin vs Configuration

| Approach | Location | Purpose | Creates JAR Index? | Framework Coupling? |
|----------|----------|---------|-------------------|---------------------|
| **Jandex Plugin** | `*-infra` modules | CDI bean discovery | Yes ✅ | Yes (acceptable in infra) |
| **`quarkus.index-dependency`** | Assembly config | DTO/VO reflection | No (runtime indexing) | No ✅ (keeps domain clean) |

---

### How It Works

1. **Build Time**: Infrastructure modules create their own indexes
   ```
   jbh-finance-infra.jar → META-INF/jandex.idx (REST endpoints, services)
   jbh-finance-domain.jar → NO index (framework-agnostic)
   jbh-finance-application.jar → NO index (framework-agnostic)
   ```

2. **Runtime**: Quarkus assembly performs two scans:
   ```
   Startup:
   ├── Scan infra JARs → finds REST endpoints, services ✅
   ├── Check quarkus.index-dependency config → indexes DTOs/VOs at runtime ✅
   └── Register all beans + enable OpenAPI for DTOs
   ```

3. **Result**:
   - All REST endpoints and CDI beans discovered ✅
   - OpenAPI/Swagger includes DTO schemas ✅
   - Domain/application remain framework-agnostic ✅

---

### Best Practice: Version Management

Manage plugin versions in root POM, use in infrastructure modules:

```xml
<!-- Root pom.xml - Version Management -->
<pluginManagement>
  <plugin>
    <groupId>io.smallrye</groupId>
    <artifactId>jandex-maven-plugin</artifactId>
    <version>3.1.2</version>
  </plugin>
</pluginManagement>

<!-- Infrastructure module pom.xml - Usage -->
<plugin>
  <groupId>io.smallrye</groupId>
  <artifactId>jandex-maven-plugin</artifactId>
  <!-- Inherits version from parent -->
  <executions>...</executions>
</plugin>
```

---

### Module Dependency Rules (Hexagonal Architecture)

**❌ NEVER in domain/application modules:**
- `io.quarkus.*` dependencies
- `org.springframework.*` dependencies
- `jakarta.ws.rs.*` (JAX-RS)
- Jandex Maven plugin

**✅ ONLY in domain/application modules:**
- JDK standard library
- Domain-specific libraries
- SLF4J API (logging facade)
- Test dependencies (JUnit, Mockito)

# ADDING NEW MODULES TO THE ASSEMBLY

1. Add new module using IntelliJ - Creates the module structure and updates parent pom
2. Run mvn clean install from root - Builds and installs all modules to local repository
3. Add module dependency to assembly - Reference the new module in jbh-assembly/pom.xml
4. Run the app - Everything should work since JARs are in local repo

The key is step 2 - mvn clean install puts the new module JAR in your local Maven repository (~/.m2/repository), making
it available for other modules to reference. Without this, Maven can't find the JAR when resolving
dependencies.

This workflow ensures the module exists before other modules try to depend on it.

1. Parent pom dependencies in root dependencyManagement:

The jbh-finance parent pom dependency in root pom.xml:65-69 is NOT necessary and should be removed. Here's why:

- Parent poms (<packaging>pom</packaging>) don't produce JARs
- They're only for aggregation and inheritance
- Only actual JAR/WAR modules should be in dependencyManagement

2. When adding new submodules like jbh-finance-domain, jbh-finance-application:

Root pom dependencyManagement: Add entries for modules that will be used as dependencies:
<dependency>
<groupId>com.jbh</groupId>
<artifactId>jbh-finance-domain</artifactId>
<version>${project.version}</version>
</dependency>

Assembly dependencies: Only add if the assembly needs that specific module. Typically:

- ✅ Add -infra modules (contain REST endpoints, repositories)
- ✅ Add -application modules (contain use cases, services)
- ❌ Don't add -domain modules directly (they're transitive dependencies)

Best practice: Remove parent pom entries from dependencyManagement and only include actual JAR-producing modules that
other modules will depend on.

## LOGGING

### slf4j-api:

- Use slf4j-api in all modules for logging calls
- The Simple Logging Facade for Java API
- Provides the logging interface/contract (Logger, LoggerFactory, etc.)
- Your code uses this API for logging calls
- Acts as an abstraction layer over actual logging implementation

# PLUGINS

The plugins are currently configured in <pluginManagement> only, which means they're available but not automatically
executed during the build lifecycle.

To make them run automatically, you need to:

1. Add plugins to <plugins> section (not just <pluginManagement>)
2. Bind them to specific lifecycle phases

Would you like me to configure them to run automatically during mvn verify? I can:

- Bind PMD to verify phase
- Bind SpotBugs to verify phase
- Set failOnViolation=true to fail builds on issues

This way, your CI/CD pipeline would automatically catch code quality issues during normal builds.

Current behavior:

- mvn verify = Tests only
- mvn pmd:pmd = Manual PMD run
- mvn spotbugs:spotbugs = Manual SpotBugs run

After configuration:

- mvn verify = Tests + PMD + SpotBugs

## PMD

`pmd-ruleset.xml` is the ruleset used by PMD to analyze the codebase.

The ruleset is located in the root directory of the project and is referenced in the root pom.xml.

PMD is a static code analysis tool that helps identify potential issues in your codebase.

PMD Benefits:

- Static Code Analysis: Detects potential bugs, dead code, suboptimal code, overcomplicated expressions
- Code Quality: Enforces coding standards and best practices
- Early Detection: Catches issues before they reach production
- Team Consistency: Ensures consistent coding patterns across your multi-module project
- CI/CD Integration: Can fail builds on violations, maintaining code quality standards

How PMD Works:

- Analyzes Java source code without compiling it
- Uses rulesets to identify violations (unused variables, empty catch blocks, etc.)
- Generates reports in various formats (HTML, XML, CSV)
- Integrates with Maven lifecycle phases
- Can run via mvn pmd:pmd or mvn pmd:check commands

### Basic PMD Commands

1. Generate PMD Report (recommended)
   mvn pmd:pmd
   Creates HTML reports in target/site/pmd.html for each module.

2. Check for Violations (fail build on issues)
   mvn pmd:check
   Analyzes code and fails build if violations found.

3. Run PMD with verify phase
   mvn verify
   Includes PMD analysis in standard build lifecycle.

Multi-Module Specific

Run on all modules:
mvn clean compile pmd:pmd

Run on specific module:
mvn pmd:pmd -pl jbh-finance

Report Locations

- Root: target/site/pmd.html
- Per module: jbh-finance/target/site/pmd.html, jbh-notification/target/site/pmd.html

Integration Options

Add to your CI/CD pipeline:
mvn clean test pmd:check

Enable build failure on violations (edit pom.xml):
<failOnViolation>true</failOnViolation>

## SPOTBUGS PLUGIN

### SpotBugs Commands

1. Generate SpotBugs Report
   mvn spotbugs:spotbugs
   Creates XML/HTML reports in target/spotbugs.xml and target/site/spotbugs.html

2. Check for Bugs (fail build)
   mvn spotbugs:check

3. GUI Report (desktop)
   mvn spotbugs:gui

Multi-Module Usage

All modules:
mvn clean compile spotbugs:spotbugs

Specific module:
mvn spotbugs:spotbugs -pl jbh-finance

Combined Static Analysis

Run both PMD and SpotBugs:
mvn clean compile pmd:pmd spotbugs:spotbugs

Add to CI/CD:
mvn clean test pmd:check spotbugs:check

Key Features Added:

- ✅ FindSecBugs plugin: Security vulnerability detection
- ✅ Max effort: Most thorough analysis
- ✅ Lombok-friendly: Excludes generated code issues
- ✅ Quarkus-optimized: Handles framework patterns
- ✅ Filter files: Include/exclude specific bug patterns

# BUILD

Development:

- mvn verify - Tests + analysis (warnings only)
- mvn pmd:pmd - Manual PMD report
- mvn spotbugs:gui - Visual SpotBugs report

# DEPLOYMENT

- Local Development:
  mvn clean verify # Runs tests + PMD + SpotBugs (non-failing)

- CI/CD Pipeline:
  mvn clean verify -Dpmd.failOnViolation=true -Dspotbugs.failOnError=true
