# MAKEFILE

Global commands:

- make help - Shows comprehensive help for all modules
- make create-all-schemas - Creates all module schemas
- make drop-all-schemas - Drops all schemas (with confirmation)
- make recreate-all-schemas - Recreates all schemas
- make check-all-connections - Tests all database connections

Module-specific help:

- make product-help - Shows products module help
- make notification-help - Shows notification module help

Individual module commands:

- make product-create-schema - Creates only products schema
- make product-drop-schema - Drops only products schema
- make notification-create-schema - Creates only notification schema
- make notification-drop-schema - Drops only notification schema

The root Makefile delegates to each module's specific .mk file, maintaining separation while providing centralized
control.

## Project Overview

I want to build a modular monolith with some boundaries (A , B, C ) and each module (A , B , C) will contain submodules
where each submodule will use hexagonal architecture.
At the end all modules belong to the same root parent project running with quarkus and only the submodule (
C.infrastructure, A.infrastructure, B.infrastructure) will the ones that exposes APIs ...
and I'd like to be able to centralize the server port for all modules.

Each module should be completely independent in how it exposes its APIs, and the assembly should just aggregate them
without imposing any specific technology choices.

A modular monolith with clean hexagonal boundaries.

One single Quarkus runtime.

Central control over server, DB, logging, health, metrics.

Easy future migration: if module-b needs to become its own service, just move its infrastructure into a new Quarkus app
module.

## How it works

- Root POM → BOM & plugin versions, no Quarkus runtime.
- Modules A/B/C → structured into hexagonal submodules.
- App module
    - Contains application.properties with quarkus.http.port=7777 → applies to the entire app
    - Is the only module with the Quarkus Maven plugin → builds the runnable JAR.
        - Depends on all infrastructure modules:
      ```xml
      <dependencies>
          <dependency>
              <groupId>com.example</groupId>
              <artifactId>jbh-products-infrastructure</artifactId>
          </dependency>
          <dependency>
              <groupId>com.example</groupId>
              <artifactId>module-b-infrastructure</artifactId>
          </dependency>
          <dependency>
              <groupId>com.example</groupId>
              <artifactId>module-c-infrastructure</artifactId>
          </dependency>
      </dependencies>

```

```bash
jbh-personal-finance/
├── pom.xml                                 (Root Parent)
├── jbh-products/
│   ├── pom.xml                            (Module A Parent)
│   ├── jbh-products-domain/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/jbh-products/domain/
│   ├── jbh-products-application/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/jbh-products/application/
│   └── jbh-products-infra/
│       ├── pom.xml
│       └── src/main/java/com/jbh/finance/jbh-products/infra/
├── module-b/
│   ├── pom.xml                            (Module B Parent)
│   ├── b-domain/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/b/domain/
│   ├── b-application/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/b/application/
│   └── b-infra/
│       ├── pom.xml
│       └── src/main/java/com/jbh/finance/b/infra/
├── module-c/
│   ├── pom.xml                            (Module C Parent)
│   ├── c-domain/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/c/domain/
│   ├── c-application/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/c/application/
│   └── c-infra/
│       ├── pom.xml
│       └── src/main/java/com/jbh/finance/c/infra/
└── assembly/
    ├── pom.xml                            (Assembly Module - Quarkus Runner)
    └── src/main/
    ├── java/com/jbh/finance/assembly/
    └── resources/
    └── application.properties
```

## CDI Bean Discovery with Jandex

### Why Jandex Plugin is Required

Quarkus uses **Jandex indexing** for CDI bean discovery in modular projects. Without proper indexing, your REST
endpoints and CDI beans won't be discovered at runtime.

### Plugin Placement Strategy

**❌ Don't put Jandex in Assembly/Root POM:**

- Assembly can only index its own classes
- Cannot retroactively index dependency JARs
- Each JAR needs its own `META-INF/jandex.idx` file

**✅ Put Jandex in Infrastructure Modules:**

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

### How It Works

1. **Build Time**: Each infrastructure module creates its own Jandex index
   ```
   jbh-products-infra.jar → contains META-INF/jandex.idx
   jbh-transaction-infra.jar → contains META-INF/jandex.idx
   ```

2. **Runtime**: Quarkus assembly scans all dependency JARs
   ```
   Startup:
   ├── Scan assembly JAR → finds AssemblyApplication
   ├── Scan jbh-products-infra.jar → finds GreetingResource ✅
   └── Register all beans from all indices
   ```

3. **Result**: All REST endpoints and CDI beans are properly discovered

### Best Practice

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

The jbh-products parent pom dependency in root pom.xml:65-69 is NOT necessary and should be removed. Here's why:

- Parent poms (<packaging>pom</packaging>) don't produce JARs
- They're only for aggregation and inheritance
- Only actual JAR/WAR modules should be in dependencyManagement

2. When adding new submodules like jbh-products-domain, jbh-products-application:

Root pom dependencyManagement: Add entries for modules that will be used as dependencies:
<dependency>
<groupId>com.jbh</groupId>
<artifactId>jbh-products-domain</artifactId>
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
mvn pmd:pmd -pl jbh-products

Report Locations

- Root: target/site/pmd.html
- Per module: jbh-products/target/site/pmd.html, jbh-notification/target/site/pmd.html

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
mvn spotbugs:spotbugs -pl jbh-products

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
