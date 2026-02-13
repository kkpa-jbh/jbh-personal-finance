# Hexagonal Architecture Guide

This directory contains comprehensive documentation for the hexagonal architecture used in the JBH Personal Finance application.

## Overview

The application follows **Hexagonal Architecture** (also known as Ports and Adapters) to maintain a clean separation between business logic and infrastructure concerns. This architecture enables:

- **Framework Independence**: Business logic remains unaware of Quarkus, Spring, or any other framework
- **Testability**: Domain and application layers can be tested without infrastructure
- **Maintainability**: Clear boundaries make the codebase easier to understand and modify
- **Flexibility**: Infrastructure components can be replaced without touching business logic
- **Microservices-Ready**: Features are organized as vertical slices that can be extracted into separate services

## Core Principles

### 1. Dependency Direction
Dependencies flow **inward** toward the domain:
- **Domain Layer**: Pure business logic, no external dependencies
- **Application Layer**: Use cases and business workflows, depends only on domain
- **Infrastructure Layer**: REST APIs, databases, external services, depends on application

### 2. Feature-Based Organization
Code is organized by **feature/bounded context** rather than technical role:
- Each feature is a self-contained vertical slice
- Features align with Domain-Driven Design bounded contexts
- Teams can own entire features independently

### 3. API Object Placement
- **DTOs** (Data Transfer Objects): Application layer, contain business data
- **Request/Response Objects**: Infrastructure layer, define API contracts
- **Entities**: Infrastructure layer, map to database tables

## Module Structure

```
jbh-finance/
├── jbh-finance-domain/           # Pure business logic (Value Objects, Entities)
├── jbh-finance-application/      # Use cases, business workflows
└── jbh-finance-infra/            # REST APIs, Database, External adapters
```

## Documentation Files

### [Hexagonal Layers](hexagonal-layers.md)
Detailed explanation of module responsibilities, dependency rules, and API object placement patterns.

**Key Topics:**
- Application module responsibilities
- Infrastructure module responsibilities
- Critical rules for Request/Response object placement
- Mapping patterns between DTOs and API objects

### [Feature Structure](feature-structure.md)
Feature-based package organization within the application module.

**Key Topics:**
- Feature package structure
- Cross-cutting infrastructure (acid/, async/, common/)
- Naming conventions
- Module exports (module-info.java)

### [REST Organization](rest-organization.md)
REST API layer structure in the infrastructure module.

**Key Topics:**
- Feature-based REST organization
- Request/Response package structure
- REST adapter patterns
- Benefits of feature-based organization

### [Dependency Rules](dependency-rules.md)
JPMS module system and Jandex indexing configuration.

**Key Topics:**
- Two-tier Jandex indexing approach
- Framework independence requirements
- Allowed/forbidden dependencies
- Native compilation support

## Technology Stack

- **Runtime**: Quarkus with GraalVM support
- **Build Tool**: Maven with multi-module configuration
- **Database**: PostgreSQL with schema-per-module approach
- **Security**: JWT with RS256 signing
- **Service Discovery**: Consul
- **API Gateway**: Quarkus-based routing

## Quick Reference

### Module Dependency Rules
```
✅ Application → Domain (allowed)
✅ Infrastructure → Application (allowed)
✅ Infrastructure → Domain (allowed)
❌ Domain → Application (forbidden)
❌ Domain → Infrastructure (forbidden)
❌ Application → Infrastructure (forbidden)
```

### Package Naming Conventions
- Feature packages: lowercase, singular (e.g., `product`, `movement`)
- Use case interfaces: `*InputPort` (e.g., `CreateProductInputPort`)
- Use case implementations: `*UseCase` (e.g., `CreateProductUseCase`)
- DTOs: `*DTO` (e.g., `ProductDTO`)
- Commands: `*Command` (e.g., `CreateProductCommand`)
- API Requests: `*Request` (e.g., `CreateProductRequest`)
- API Responses: `*Response` (e.g., `ProductResponse`)
