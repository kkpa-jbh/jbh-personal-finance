# jbh-personal-finance
Modular monolith to handle personal finance

# MAKEFILE

Global commands:
- make help - Shows comprehensive help for all modules
- make create-all-schemas - Creates all module schemas
- make drop-all-schemas - Drops all schemas (with confirmation)
- make recreate-all-schemas - Recreates all schemas
- make check-all-connections - Tests all database connections

Module-specific help:
- make account-help - Shows account module help
- make notification-help - Shows notification module help

Individual module commands:
- make account-create-schema - Creates only account schema
- make account-drop-schema - Drops only account schema
- make notification-create-schema - Creates only notification schema
- make notification-drop-schema - Drops only notification schema

The root Makefile delegates to each module's specific .mk file, maintaining separation while providing centralized control.

## Project Overview

I want to build a modular monolith with some boundaries (A , B, C ) and each module (A , B , C) will contain submodules where each submodule will use hexagonal architecture.
At the end all modules belong to the same root parent project running with quarkus and only the submodule (C.infrastructure, A.infrastructure, B.infrastructure) will the ones that exposes APIs ...
and I'd like to be able to centralize the server port for all modules. 

Each module should be completely independent in how it exposes its APIs, and the assembly should just aggregate them without imposing any specific technology choices.

A modular monolith with clean hexagonal boundaries.

One single Quarkus runtime.

Central control over server, DB, logging, health, metrics.

Easy future migration: if module-b needs to become its own service, just move its infrastructure into a new Quarkus app module.

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
            <artifactId>jbh-account-infrastructure</artifactId>
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
├── jbh-account/
│   ├── pom.xml                            (Module A Parent)
│   ├── jbh-account-domain/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/jbh-account/domain/
│   ├── jbh-account-application/
│   │   ├── pom.xml
│   │   └── src/main/java/com/jbh/finance/jbh-account/application/
│   └── jbh-account-infra/
│       ├── pom.xml
│       └── src/main/java/com/jbh/finance/jbh-account/infra/
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

Quarkus uses **Jandex indexing** for CDI bean discovery in modular projects. Without proper indexing, your REST endpoints and CDI beans won't be discovered at runtime.

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
   jbh-account-infra.jar → contains META-INF/jandex.idx
   jbh-transaction-infra.jar → contains META-INF/jandex.idx
   ```

2. **Runtime**: Quarkus assembly scans all dependency JARs
   ```
   Startup:
   ├── Scan assembly JAR → finds AssemblyApplication
   ├── Scan jbh-account-infra.jar → finds GreetingResource ✅
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

The key is step 2 - mvn clean install puts the new module JAR in your local Maven repository (~/.m2/repository), making it available for other modules to reference. Without this, Maven can't find the JAR when resolving
dependencies.

This workflow ensures the module exists before other modules try to depend on it.


1. Parent pom dependencies in root dependencyManagement:

The jbh-account parent pom dependency in root pom.xml:65-69 is NOT necessary and should be removed. Here's why:

- Parent poms (<packaging>pom</packaging>) don't produce JARs
- They're only for aggregation and inheritance
- Only actual JAR/WAR modules should be in dependencyManagement

2. When adding new submodules like jbh-account-domain, jbh-account-application:

Root pom dependencyManagement: Add entries for modules that will be used as dependencies:
<dependency>
<groupId>com.jbh</groupId>
<artifactId>jbh-account-domain</artifactId>
<version>${project.version}</version>
</dependency>

Assembly dependencies: Only add if the assembly needs that specific module. Typically:
- ✅ Add -infra modules (contain REST endpoints, repositories)
- ✅ Add -application modules (contain use cases, services)
- ❌ Don't add -domain modules directly (they're transitive dependencies)

Best practice: Remove parent pom entries from dependencyManagement and only include actual JAR-producing modules that other modules will depend on.