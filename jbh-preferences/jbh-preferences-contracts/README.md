# JBH Preferences Contracts

This module provides shared contracts (interfaces, DTOs, exceptions) for inter-module communication with the Preferences module. It enables other modules to fetch user preferences via direct method calls instead of HTTP API calls.

## Architecture

```
┌─────────────────────────┐     ┌──────────────────────────────┐
│  Consumer Module        │     │  Preferences Module          │
│  (e.g., notification)   │     │                              │
│                         │     │  ┌────────────────────────┐  │
│  ┌───────────────────┐  │     │  │ PreferencesLookupAdapter│  │
│  │ DirectPreferences │──┼─────┼──│ (implements Port)      │  │
│  │ Adapter           │  │     │  └───────────┬────────────┘  │
│  └───────────────────┘  │     │              │               │
│           │             │     │              ▼               │
│           ▼             │     │  ┌────────────────────────┐  │
│  ┌───────────────────┐  │     │  │ GetUserPreferences     │  │
│  │ PreferencesLookup │  │     │  │ InputPort              │  │
│  │ Port (interface)  │◄─┼─────┼──│                        │  │
│  └───────────────────┘  │     │  └────────────────────────┘  │
└─────────────────────────┘     └──────────────────────────────┘
        contracts module ───────────────────┘
```

## Available Entry Point

### `PreferencesLookupPort`

The main interface for fetching user preferences from other modules.

```java
public interface PreferencesLookupPort {

    // Get preferences (throws exception if not found)
    UserPreferencesData getPreferences(UUID userId) throws PreferencesLookupException;

    // Find preferences (returns Optional.empty() if not found)
    Optional<UserPreferencesData> findPreferences(UUID userId) throws PreferencesLookupException;

    // Convenience method with fallback to default language
    default String getLanguageCode(UUID userId);
}
```

### `UserPreferencesData`

Simplified DTO returned by the port:

```java
public record UserPreferencesData(
    UUID userId,
    String languageCode,    // e.g., "en", "es"
    String currencyCode,    // e.g., "USD", "COP"
    BigDecimal savingsGoal,
    UUID defaultAccountId   // nullable
) { }
```

### `PreferencesLookupException`

Exception thrown when preferences lookup fails:

```java
public class PreferencesLookupException extends Exception {
    UUID getUserId();       // The user ID that failed lookup
    boolean hasUserId();    // Check if user ID is available
}
```

## Usage Example

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.jbh</groupId>
    <artifactId>jbh-preferences-contracts</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. Inject the Port

```java
@ApplicationScoped
public class MyService {

    private final PreferencesLookupPort preferencesLookupPort;

    @Inject
    public MyService(PreferencesLookupPort preferencesLookupPort) {
        this.preferencesLookupPort = preferencesLookupPort;
    }

    public void doSomething(UUID userId) {
        // Option 1: Get language with automatic fallback
        String language = preferencesLookupPort.getLanguageCode(userId);

        // Option 2: Get full preferences (may throw)
        try {
            UserPreferencesData prefs = preferencesLookupPort.getPreferences(userId);
            // Use prefs.currencyCode(), prefs.savingsGoal(), etc.
        } catch (PreferencesLookupException e) {
            // Handle missing preferences
        }

        // Option 3: Optional-based lookup
        Optional<UserPreferencesData> prefs = preferencesLookupPort.findPreferences(userId);
        prefs.ifPresent(p -> {
            // Use preferences
        });
    }
}
```

### 3. Update module-info.java (if using JPMS)

```java
module your.module {
    requires jbh.preferences.contracts;
}
```

## Implementation Details

The `PreferencesLookupPort` is implemented by `PreferencesLookupAdapter` in the `jbh-preferences-infra` module. This adapter:

- Delegates to `GetUserPreferencesInputPort` (application layer)
- Maps internal `UserPreferencesDTO` to contract `UserPreferencesData`
- Wraps internal `BusinessException` into `PreferencesLookupException`

## Current Consumers

| Module | Usage |
|--------|-------|
| `jbh-notification-infra` | Resolves user language preference for email templates |

## Design Decisions

1. **Why a contracts module?** Follows the existing `jbh-notification-contracts` pattern. Allows modules to depend on contracts without pulling in the full application layer.

2. **Why `UserPreferencesData` instead of reusing `UserPreferencesDTO`?** The DTO uses domain value objects (`Language`, `Currency`). The contracts module provides primitive types (`String`) to avoid exposing internal domain types.

3. **Why wrap exceptions?** `PreferencesLookupException` provides a clean boundary. Consuming modules shouldn't depend on internal exception types like `BusinessException`.

4. **Default language fallback?** The `getLanguageCode()` method returns `"es"` (Spanish) as default when preferences are not found, ensuring notifications are never blocked.
