# Maven Enforcer Plugin Configuration

Optimal Configuration Structure:

pluginManagement Section:

- ✅ Plugin version (${maven-enforcer-plugin.version})
- ✅ All configuration rules (Maven version, Java version, dependency rules)
- ✅ Available for direct execution (mvn enforcer:enforce)

plugins Section:

- ✅ Only execution definition (when to run: validate phase)
- ✅ No duplicate configuration - inherits from pluginManagement
- ✅ Clean and concise

Benefits of This Approach:

1. DRY Principle - Configuration defined once in pluginManagement
2. Inheritance - Child modules automatically get the rules
3. Maintainability - Change rules in one place
4. Flexibility - Direct execution and lifecycle execution both work
5. Clean POMs - No configuration duplication

## Overview

The Maven Enforcer Plugin has been configured at the module parent level (`jbh-products` and `jbh-notification`) to
provide dependency governance and build consistency across the multi-module Quarkus project.

## What is the Maven Enforcer Plugin?

The Maven Enforcer Plugin provides goals to control certain environmental constraints such as Maven version, JDK
version, and OS family along with many more built-in rules and user-created rules.
It helps ensure that builds are consistent across different environments and prevents common issues related to
dependency management.

## Strategic Placement

The plugin is configured in the **module parent POMs** (`jbh-products/pom.xml` and `jbh-notification/pom.xml`) rather
than the root POM. This architectural decision supports:

### Benefits of Module-Level Configuration:

1. **Microservice Readiness** - Each module can be independently extracted as a microservice with its own governance
   rules
2. **Bounded Context Isolation** - Domain-specific dependency rules without affecting other modules
3. **Focused Conflict Resolution** - Issues are caught at the appropriate module boundary
4. **Independent Evolution** - Modules can have different version requirements and security policies

## Configured Rules

### 1. Maven Version Enforcement

```xml

<requireMavenVersion>
  <version>[3.8.1,)</version>
  <message>Maven 3.8.1 or higher is required for this project</message>
</requireMavenVersion>
```

**Purpose**: Ensures build consistency across development environments and CI/CD pipelines.

### 2. Java Version Enforcement

```xml

<requireJavaVersion>
  <version>[21,)</version>
  <message>Java 21 or higher is required for this project</message>
</requireJavaVersion>
```

**Purpose**: Enforces the minimum Java version (21) required for Quarkus features and language constructs.

### 3. Dependency Convergence

```xml

<dependencyConvergence/>
```

**Purpose**: Prevents "dependency hell" by ensuring all transitive dependencies converge to the same version, avoiding
ClassNotFoundException and NoSuchMethodError at runtime.

### 4. Duplicate Dependency Prevention

```xml

<banDuplicatePomDependencyVersions/>
```

**Purpose**: Detects duplicate dependency declarations with different versions in the same POM, preventing unpredictable
behavior.

### 5. Security-Based Dependency Banning

```xml

<bannedDependencies>
  <excludes>
    <exclude>log4j:log4j:*:jar:compile</exclude>
    <exclude>commons-logging:commons-logging:*:jar:compile</exclude>
  </excludes>
  <message>These dependencies are banned due to security vulnerabilities or conflicts</message>
</bannedDependencies>
```

**Purpose**: Prevents known vulnerable or conflicting dependencies from being included in the build.

## Execution Phase

The enforcer runs during the **validate** phase, which occurs before compilation. This ensures that environment and
dependency issues are caught as early as possible in the build lifecycle.

## Benefits for Multi-Module Quarkus Projects

### 1. **Build Consistency**

- Guarantees same Maven and Java versions across all developers and CI environments
- Prevents "works on my machine" issues

### 2. **Dependency Safety**

- Early detection of version conflicts before they cause runtime issues
- Protection against known security vulnerabilities
- Prevents accidental inclusion of conflicting logging frameworks

### 3. **Future-Proof Architecture**

- Each module maintains its own governance rules
- Smooth transition when extracting modules as independent microservices
- Domain-specific policies without cross-module interference

### 4. **Development Experience**

- Fast feedback on environment issues
- Clear error messages guiding developers to solutions
- Automated prevention of common configuration mistakes

## Customization

Each module can extend or override these rules based on specific requirements:

- Add module-specific banned dependencies
- Implement custom rules for business logic constraints
- Configure different version requirements for different bounded contexts

This configuration establishes a solid foundation for maintaining code quality and consistency as the project evolves
toward a microservices architecture.