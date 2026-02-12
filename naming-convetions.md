# Services Names

| Layer                 | Naming Style                                        |
|-----------------------|-----------------------------------------------------|
| Application Lifecycle | `UserLifecycleService`                              |
| Application Workflow  | `RegisterUserService`                               |
| Domain Service        | `PricingService`, `FraudDetector`, `DiscountPolicy` |
| Entity                | `User`, `Order`, `Invoice`                          |
| Repository Port       | `UserRepository`                                    |

## Domain Module

Domain services must be named after business capabilities, not technical roles.

Use:

```
<BusinessCapability>Service
```

What to avoid

```
UserService
OrderService
EntityService
UserManager
UserHelper
UserDomainService
```

## Application Module

### Golden Rule

- If a method name sounds like a SQL operation → it belongs in Lifecycle.

- If a method name sounds like a business sentence → it belongs in Workflow.

### Naming Strategy

- Use `Lifecycle` for CRUD operations over entities.

```
UserLifecycleService
ProductLifecycleService
```

- Use `UseCase-driven` naming for workflows services. Workflows should express business meaning, not retrieval mechanics.

```
RegisterUserService
PlaceOrderService
ApproveLoanService
```

# CRUD Operation Prefixes

## Create Operations

Create - Primary creation
Add - Adding relationships/items
Register - Business-specific creation

## Query Operations

Query - Your preferred prefix ✅
Find - Alternative option

## Update Operations

Update - General modifications
Modify - Specific changes
Change - State transitions

## Delete Operations

Delete - Hard deletion
Remove - Soft deletion/removal
Archive - Business deletion