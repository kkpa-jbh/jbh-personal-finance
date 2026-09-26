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

Each infrastructure module defines its own route constants:

| Module                   | Class                                         | Value                                   |
|--------------------------|-----------------------------------------------|-----------------------------------------|
| `jbh-finance-infra`      | `ApiConstants.BASE_API_PATH`, `FinanceApiRoutes` | `/jbh-api/finance`                   |
| `jbh-preferences-infra`  | `PreferencesApiConstants.BASE_API_PATH`, `PreferencesApiRoutes` | `/jbh-api/preferences` (`/v1`, `/team-preferences/v1`) |
| `jbh-notification-infra` | `NotificationRoutes.NOTIFICATIONS_API_PATH_V1` | `/jbh-api/notifications/v1`            |

### Benefits

1. **Domain Separation**: Each bounded context has its own API namespace
2. **Independent Evolution**: Modules can change their API structure without affecting others
3. **Microservice Ready**: If a module is extracted to a separate service, its API paths remain unchanged
4. **Clear Ownership**: API paths clearly indicate which module owns the endpoint

## API Structure

All modules follow the pattern `/jbh-api/{domain}/{resource}`:

```
/jbh-api/finance/products
/jbh-api/finance/products/movements
/jbh-api/finance/products/transfers
/jbh-api/finance/product-types
/jbh-api/finance/categories
/jbh-api/preferences/v1
/jbh-api/preferences/team-preferences/v1/{teamId}
/jbh-api/notifications/v1
```

## Gateway routing

Clients never call port 7777 directly. `jbh-gateway` (port 8080) sends these prefixes here with `lb://jbh-personal-finance`:
`/jbh-api/finance/**`, `/jbh-api/preferences/**`, `/jbh-api/notifications/**`.

**When you add a new top-level prefix** (for example `/jbh-api/budgets`), you must also add it to a route in
`jbh-gateway/src/main/resources/application.yml`, and update the system map in `kkpa-jbh/README.md`.

## Adding a New Module

When creating a new infrastructure module:

1. Create an `ApiConstants` class in the module:

```java
package com.jbh.newmodule;

public final class ApiConstants {

  public static final String BASE_PATH = "/jbh-api/{domain-name}";

  private ApiConstants() {
  }
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
