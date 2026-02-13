# JPMS and Jandex Configuration

## Overview

This document explains the two-tier indexing strategy that maintains hexagonal architecture principles while enabling Quarkus CDI and reflection capabilities.

**CRITICAL GOAL**: Keep domain/application layers framework-agnostic while enabling infrastructure runtime discovery.

## Two-Tier Indexing Approach

### 1. Infrastructure Modules - Jandex Plugin

Infrastructure modules (`*-infra`) use the **Jandex Maven Plugin** to index CDI beans.

#### Configuration

```xml
<!-- In jbh-xxx-infra/pom.xml -->
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

#### Purpose

Index CDI beans for runtime discovery:
- REST endpoints (`@Path`, `@GET`, `@POST`, etc.)
- Services (`@ApplicationScoped`, `@RequestScoped`, `@Singleton`)
- Repositories, adapters, and infrastructure components

#### Result

Each infra JAR contains `META-INF/jandex.idx` for CDI bean discovery.

### 2. Domain/Application Modules - Index Dependency Configuration

Domain and application modules use **`quarkus.index-dependency`** configuration to index non-CDI classes.

#### Configuration

```properties
# In jbh-z-assembly/src/main/resources/application.properties

# Index finance domain module
quarkus.index-dependency.products-domain.group-id=com.jbh
quarkus.index-dependency.products-domain.artifact-id=jbh-finance-domain

# Index finance application module
quarkus.index-dependency.products-application.group-id=com.jbh
quarkus.index-dependency.products-application.artifact-id=jbh-finance-application

# Repeat for each domain/application module
quarkus.index-dependency.iam-domain.group-id=com.jbh
quarkus.index-dependency.iam-domain.artifact-id=jbh-iam-domain

quarkus.index-dependency.iam-application.group-id=com.jbh
quarkus.index-dependency.iam-application.artifact-id=jbh-iam-application
```

#### Purpose

Index non-CDI classes for OpenAPI/Swagger and reflection:
- DTOs (Data Transfer Objects)
- VOs (Value Objects)
- Domain entities
- Command objects

#### Result

Domain/application modules remain framework-agnostic with **NO Quarkus dependencies**.

## Why This Matters

### Benefits Table

| Aspect                     | Benefit                                                           |
|----------------------------|-------------------------------------------------------------------|
| **Hexagonal Architecture** | Domain/application layers have ZERO infrastructure dependencies   |
| **Framework Independence** | Can switch from Quarkus to Spring without touching business logic |
| **Clean Architecture**     | Infrastructure concerns isolated to infra/assembly layers         |
| **CDI Discovery**          | Infra beans properly discovered at runtime                        |
| **OpenAPI Generation**     | DTOs/VOs indexed for Swagger documentation                        |
| **Native Compilation**     | All classes properly registered for GraalVM reflection            |

### Hexagonal Architecture Compliance

```
┌─────────────────────────────────────┐
│   Domain/Application Modules        │
│   - NO framework dependencies       │
│   - Indexed via quarkus.index-dep   │
│   - Pure business logic             │
└─────────────────────────────────────┘
              ▲
              │ depends on
              │
┌─────────────────────────────────────┐
│   Infrastructure Modules            │
│   - Quarkus, JAX-RS, Hibernate      │
│   - Indexed via Jandex plugin       │
│   - Framework-specific code         │
└─────────────────────────────────────┘
```

## Module Dependency Rules

### ❌ NEVER Allow in Domain/Application Modules

**Framework Dependencies**:
- Quarkus dependencies (`io.quarkus.*`)
- Spring dependencies (`org.springframework.*`)
- JAX-RS annotations (`jakarta.ws.rs.*`)
- JPA/Hibernate annotations in domain entities
- CDI annotations in domain objects
- Any framework-specific code

**Why**: Domain and application layers must be framework-agnostic to maintain hexagonal architecture principles.

### ✅ ONLY Allow in Domain/Application Modules

**Standard Libraries**:
- JDK standard library (`java.*`, `javax.*` from JDK)
- Domain-specific libraries (validation, money types, date libraries)
- SLF4J API (logging facade only, NOT implementation)
- Test dependencies (JUnit, Mockito, AssertJ)

**Why**: These dependencies don't couple business logic to infrastructure frameworks.

### ✅ Infrastructure Dependencies Belong In

**Infrastructure Modules (`*-infra`)**:
- Quarkus extensions (`quarkus-rest`, `quarkus-hibernate-orm`)
- JAX-RS annotations (`@Path`, `@GET`, `@POST`)
- Hibernate/JPA (`@Entity`, `@Table`, `@Column`)
- CDI (`@ApplicationScoped`, `@Inject`)
- JSON serialization (`@JsonProperty`, Jackson)

**Assembly Module (`jbh-z-assembly`)**:
- Quarkus bootstrap configuration
- Application properties
- Runtime configuration

**Why**: Infrastructure code is isolated and can be replaced without touching business logic.

## Practical Examples

### ✅ CORRECT: Framework-Agnostic Application Module

```java
// Domain entity - NO framework annotations
package com.jbh.finance.domain;

public class Product {
  private final String id;
  private final String name;

  // Pure business logic, no framework code
  public Money calculateBalance() { ... }
}

// Application DTO - NO framework annotations
package com.jbh.finance.application.feature.product.dto;

public record ProductDTO(String id, String name) {}

// Use case - NO framework annotations
package com.jbh.finance.application.feature.product.usecases;

public class CreateProductUseCase implements CreateProductInputPort {
  // Pure business logic
  public ProductDTO execute(CreateProductCommand command) { ... }
}
```

### ❌ INCORRECT: Framework-Coupled Application Module

```java
// ❌ BAD: JAX-RS annotation in application layer
package com.jbh.finance.application.feature.product.dto;

import jakarta.ws.rs.QueryParam;  // ❌ Infrastructure leak

public record ProductDTO(
    @QueryParam("id") String id,  // ❌ HTTP concern in business layer
    String name
) {}

// ❌ BAD: CDI annotation in use case
package com.jbh.finance.application.feature.product.usecases;

import jakarta.enterprise.context.ApplicationScoped;  // ❌ Framework dependency

@ApplicationScoped  // ❌ Infrastructure concern in business layer
public class CreateProductUseCase {
  // Business logic should be framework-agnostic
}
```

### ✅ CORRECT: Infrastructure Module with Framework Code

```java
// ✅ GOOD: JAX-RS annotations in infrastructure layer
package com.jbh.finance.infra.adapters.in.rest.product;

import jakarta.ws.rs.*;  // ✅ OK in infra module

@Path("/products")
@ApplicationScoped
public class ProductRestAdapter {

  @Inject  // ✅ CDI OK in infra module
  CreateProductInputPort createProduct;

  @POST
  public ProductResponse create(CreateProductRequest request) {
    // Adapter code with framework annotations
  }
}

// ✅ GOOD: JPA annotations in persistence layer
package com.jbh.finance.infra.adapters.out.persistence.entity;

import jakarta.persistence.*;  // ✅ OK in infra module

@Entity
@Table(name = "products", schema = "finance")
public class ProductEntity {
  @Id
  private String id;

  @Column(name = "name")
  private String name;
}
```

## Migration Path

If you need to switch frameworks (e.g., Quarkus → Spring):

1. **Keep**: Domain and application modules (no changes needed)
2. **Replace**: Infrastructure modules only
3. **Update**: Assembly module configuration
4. **Result**: Business logic remains untouched

This is only possible if domain/application layers remain framework-agnostic.

## Verification Checklist

### Domain Module Verification

- [ ] No `io.quarkus.*` imports
- [ ] No `org.springframework.*` imports
- [ ] No `jakarta.ws.rs.*` imports
- [ ] No `jakarta.enterprise.*` imports
- [ ] Only JDK and domain-specific libraries
- [ ] No Jandex plugin in `pom.xml`

### Application Module Verification

- [ ] No framework dependencies
- [ ] Only depends on domain module
- [ ] No REST/HTTP concerns
- [ ] No persistence annotations
- [ ] No CDI annotations
- [ ] No Jandex plugin in `pom.xml`
- [ ] Module indexed via `quarkus.index-dependency` in assembly

### Infrastructure Module Verification

- [ ] Can use any framework dependencies
- [ ] Has Jandex plugin in `pom.xml`
- [ ] Generates `META-INF/jandex.idx`
- [ ] Depends on application module
- [ ] Contains REST/DB/external adapters

## Summary

**Two-Tier Strategy**:
1. **Infrastructure modules**: Use Jandex plugin for CDI bean discovery
2. **Domain/application modules**: Use `quarkus.index-dependency` for reflection support

**Result**:
- Hexagonal architecture principles maintained
- Framework independence preserved
- Quarkus runtime capabilities enabled
- Native compilation supported
