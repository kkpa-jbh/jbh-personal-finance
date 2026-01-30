# API Routing Strategy

## Overview

This project follows a **modular monolith** architecture where each bounded context (module) exposes its own REST API with an independent base path.

## Why Per-Module API Prefixes?

### The Problem with Global Prefixes

Quarkus provides a `quarkus.rest.path` configuration property that sets a global prefix for all JAX-RS endpoints. However, this approach has limitations in a modular monolith:

```properties
# This would apply to ALL modules in the application
quarkus.rest.path=/jbh-api/finance
```

If we used this global setting:
- **All modules** would share the same `/jbh-api/finance` prefix
- The notification module's `/test-notification` endpoint would become `/jbh-api/finance/test-notification`
- Domain boundaries become blurred in the API structure
- Modules cannot be independently configured or extracted

### The Solution: Per-Module Constants

Each infrastructure module defines its own `ApiConstants` class with a `BASE_PATH` or `BASE_API_PATH` constant:

| Module | Constant | Value |
|--------|----------|-------|
| `jbh-products-infra` | `BASE_API_PATH` | `/jbh-api/finance` |
| `jbh-notification-infra` | `BASE_PATH` | `/jbh-api/notifications` |

### Benefits

1. **Domain Separation**: Each bounded context has its own API namespace
2. **Independent Evolution**: Modules can change their API structure without affecting others
3. **Microservice Ready**: If a module is extracted to a separate service, its API paths remain unchanged
4. **Clear Ownership**: API paths clearly indicate which module owns the endpoint

## API Structure

All modules follow the pattern `/jbh-api/{domain}/{resource}`:

```
/jbh-api/finance/products
/jbh-api/finance/products/{id}/movements
/jbh-api/finance/product-types
/jbh-api/notifications/test-notification/status
```

## Adding a New Module

When creating a new infrastructure module:

1. Create an `ApiConstants` class in the module:

```java
package com.jbh.newmodule;

public final class ApiConstants {
    public static final String BASE_PATH = "/jbh-api/{domain-name}";

    private ApiConstants() {}
}
```

2. Reference this constant in your REST adapters:

```java
@Path(ApiConstants.BASE_PATH + "/resource")
public class MyRestAdapter {
    // ...
}
```

3. For multiple routes, create a `Routes` class that builds on the base path:

```java
public class MyModuleRoutes {
    public static final String RESOURCE_PATH = ApiConstants.BASE_PATH + "/resource";
    public static final String OTHER_PATH = ApiConstants.BASE_PATH + "/other";
}
```
