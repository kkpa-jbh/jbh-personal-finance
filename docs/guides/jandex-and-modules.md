# Jandex Indexing Strategy (Hexagonal Architecture)

Moved from the root README. How Quarkus finds beans and DTOs across the hexagonal modules,
and how to add a new module.


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

## Adding a new module to the assembly

1. Add new module using IntelliJ - Creates the module structure and updates parent pom
2. Run mvn clean install from root - Builds and installs all modules to local repository
3. Add module dependency to assembly - Reference the new module in jbh-z-assembly/pom.xml
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
