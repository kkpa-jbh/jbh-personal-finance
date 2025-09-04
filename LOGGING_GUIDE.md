# JBH Personal Finance - Logging Guide

## Overview

This guide explains how to use the centralized logging system across all modules in the JBH Personal Finance
application.

The logging system uses **SLF4J + Logback** with **MDC (Mapped Diagnostic Context)** support for tracking requests
across modules.

## Architecture

### Components

1. **SLF4J API**: Framework-agnostic logging facade
2. **Logback Classic**: High-performance implementation
3. **MDC Context**: For tracking request metadata
4. **Logback Configuration**: Centralized in `logback-spring.xml`
5. **Utility Classes**: For easy logging setup

### Module Structure

```
jbh-personal-finance/
├── pom.xml                    # Root POM with logging dependencies
├── jbh-account/
│   ├── jbh-account-domain/    # SLF4J API only
│   ├── jbh-account-application/ # SLF4J API + Utility classes
│   └── jbh-account-infra/     # Full logging implementation
└── jbh-notification/
    └── jbh-notification-infra/ # Full logging implementation
```

## Dependencies Added

### Root POM (`pom.xml`)

```xml

<properties>
  <slf4j.version>2.0.13</slf4j.version>
  <logback.version>1.5.6</logback.version>
</properties>

<dependencyManagement>
<dependencies>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>${slf4j.version}</version>
  </dependency>
  <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>${logback.version}</version>
  </dependency>
  <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-core</artifactId>
    <version>${logback.version}</version>
  </dependency>
</dependencies>
</dependencyManagement>
```

### Module POMs

Each module includes the SLF4J API dependency:

```xml

<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
</dependency>
```

## Configuration

### Logback Configuration (`logback-spring.xml`)

Located in: `jbh-account-infra/src/main/resources/logback-spring.xml`

**Key Features:**

- **MDC Support**: Tracks `trackingId`, `userId`, `accountId`, `transactionId`, `module`
- **Multiple Appenders**: Console, File, JSON, Async
- **Rolling Policy**: Time-based with size limits
- **Profile-based Configuration**: Different settings for dev/test/prod

**Log Pattern:**

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{trackingId:-NO-TRACKING}] [%X{accountId:-}] [%X{userId:-}] [%X{transactionId:-}] [%X{module:-}] %logger{36} - %msg%n
```

## Utility Classes

### 1. LoggingContext

**Purpose**: Manage MDC values for request context tracking

**Location**: `com.jbh.account_app.common.logging.LoggingContext`

**Key Methods:**

```java
// Set context values
LoggingContext.setTrackingId("abc-123");
LoggingContext.

setUserId("user-456");
LoggingContext.

setAccountId("acc-789");

// Builder pattern
LoggingContext.

builder()
    .

trackingId("abc-123")
    .

userId("user-456") 
    .

accountId("acc-789")
    .module("account-application")
    .execute(()->{
    // Your business logic here
    });

// Automatic cleanup
    LoggingContext.withTrackingId("abc-123",()->{
    // Business logic - context auto-cleared after
    });
```

### 2. LoggerFactory

**Purpose**: Create loggers with structured logging support

**Location**: `com.jbh.account_app.common.logging.LoggerFactory`

**Usage:**

```java
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

// Or structured logger
private static final StructuredLogger structuredLogger =
    LoggerFactory.getStructuredLogger(MyClass.class);
```

### 3. LoggingFilter

**Purpose**: Automatically set up logging context for HTTP requests

**Location**: `com.jbh.account_infra.common.logging.LoggingFilter`

**Features:**

- Auto-generates `trackingId` and `requestId`
- Extracts `userId` from headers
- Logs request/response timing
- Handles X-Forwarded-For for IP tracking

## Usage Examples

### 1. Basic Logging in Domain Layer

```java
package com.jbh.accounts_mgmt.accounts.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountDomain {

  private static final Logger logger = LoggerFactory.getLogger(AccountDomain.class);

  public void syncBalances(TransactionDomain transaction) {
    logger.debug("Syncing balance for account: {}, transaction amount: {}",
        getId().value(), transaction.getAmount());

    // Business logic

    logger.info("Balance updated successfully to: {}", getBalance());
  }
}
```

### 2. Application Layer with Full Context

```java
package com.jbh.account_app.accounts.ports.input;

import com.jbh.account_app.common.logging.LoggerFactory;
import com.jbh.account_app.common.logging.LoggingContext;

public class AddTransactionInputPort {

  private static final Logger logger = LoggerFactory.getLogger(AddTransactionInputPort.class);

  @Override
  public void addTransaction(AccountId accountId, AddTransactionWithDateAmount requestVO) {
    LoggingContext.builder()
        .accountId(accountId.value())
        .userId(requestVO.userId())
        .module("account-application")
        .execute(() -> {
          logger.info("Starting transaction addition for account: {}", accountId.value());

          try {
            // Business logic
            TransactionDomain txn = createTransaction(requestVO);
            LoggingContext.setTransactionId(txn.getId().value());

            logger.info("Transaction created successfully: {}", txn.getId().value());
          } catch (Exception e) {
            logger.error("Failed to add transaction for account: {}", accountId.value(), e);
            throw e;
          }
        });
  }
}
```

### 3. Infrastructure Layer with HTTP Context

```java
package com.jbh.account_infra.controllers;

import com.jbh.account_app.common.logging.LoggerFactory;
import com.jbh.account_app.common.logging.LoggingContext;

@RestController
public class AccountController {

  private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

  @PostMapping("/accounts/{accountId}/transactions")
  public ResponseEntity<Void> addTransaction(
      @PathVariable UUID accountId,
      @RequestBody TransactionRequest request) {

    // Context already set by LoggingFilter, just add specific details
    LoggingContext.setModule("account-controller");

    logger.info("Received transaction request for account: {}", accountId);

    try {
      addTransactionUseCase.addTransaction(AccountId.of(accountId), request.toVO());
      logger.info("Transaction processed successfully");
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      logger.error("Failed to process transaction", e);
      return ResponseEntity.status(500).build();
    }
  }
}
```

### 4. Async Processing with Context Propagation

```java
public class AsyncTransactionProcessor {

  private static final Logger logger = LoggerFactory.getLogger(AsyncTransactionProcessor.class);

  public CompletableFuture<Void> processAsync(TransactionDomain transaction) {
    // Capture current context
    String currentTrackingId = LoggingContext.getTrackingId();
    String currentAccountId = LoggingContext.getAccountId();

    return CompletableFuture.runAsync(() -> {
      // Restore context in async thread
      LoggingContext.builder()
          .trackingId(currentTrackingId)
          .accountId(currentAccountId)
          .transactionId(transaction.getId().value())
          .module("async-processor")
          .execute(() -> {
            logger.info("Processing transaction asynchronously: {}",
                transaction.getId().value());

            // Async processing logic
            processTransaction(transaction);

            logger.info("Async transaction processing completed");
          });
    });
  }
}
```

## Log Output Examples

### Console Output

```
2025-01-15 14:30:15.123 [http-nio-8080-exec-1] INFO  [abc-123-def] [acc-789] [user-456] [txn-001] [account-controller] c.j.a.i.controllers.AccountController - Received transaction request for account: acc-789
2025-01-15 14:30:15.124 [http-nio-8080-exec-1] INFO  [abc-123-def] [acc-789] [user-456] [] [account-application] c.j.a.a.p.input.AddTransactionInputPort - Starting transaction addition for account: acc-789
2025-01-15 14:30:15.125 [http-nio-8080-exec-1] DEBUG [abc-123-def] [acc-789] [user-456] [txn-001] [account-application] c.j.a.a.p.input.AddTransactionInputPort - Created transaction domain with ID: txn-001
```

### File Output (Structured)

```
2025-01-15 14:30:15.123 [http-nio-8080-exec-1] INFO  [abc-123-def] [acc-789] [user-456] [txn-001] [account-controller] c.j.a.i.controllers.AccountController - Received transaction request for account: acc-789
```

### JSON Output

```json
{
  "@timestamp": "2025-01-15T14:30:15.123Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "c.j.a.i.controllers.AccountController",
  "message": "Received transaction request for account: acc-789",
  "mdc": {
    "trackingId": "abc-123-def",
    "accountId": "acc-789",
    "userId": "user-456",
    "transactionId": "txn-001",
    "module": "account-controller"
  },
  "application": "jbh-personal-finance"
}
```

## Best Practices

### 1. Always Use MDC Context

```java
// ✅ Good - Use builder pattern for automatic cleanup
LoggingContext.builder()
    .

trackingId(trackingId)
    .

accountId(accountId)
    .

execute(() ->{
    // Business logic
    });

// ❌ Bad - Manual setup without cleanup
    LoggingContext.

setTrackingId(trackingId);
// Business logic - context might leak
```

### 2. Module-Specific Logging

```java
// ✅ Good - Set module context
LoggingContext.setModule("account-domain");
logger.

info("Processing in domain layer");

// ✅ Good - Different modules set their context
LoggingContext.

setModule("account-application");
LoggingContext.

setModule("account-infrastructure");
```

### 3. Structured Logging

```java
// ✅ Good - Use parameterized messages
logger.info("Transaction created with ID: {} for account: {}",txnId, accountId);

// ❌ Bad - String concatenation
logger.

info("Transaction created with ID: "+txnId +" for account: "+accountId);
```

### 4. Exception Logging

```java
// ✅ Good - Include exception in log
logger.error("Failed to process transaction for account: {}",accountId, exception);

// ❌ Bad - Log message without exception details
logger.

error("Failed to process transaction");
```

## Configuration Options

### Environment Variables

- `LOG_LEVEL`: Set logging level (DEBUG, INFO, WARN, ERROR)
- `LOG_FILE`: Path to log file (default: `/tmp/jbh-personal-finance.log`)

### Application Properties

```properties
# Quarkus logging properties
quarkus.log.level=INFO
quarkus.log.console.enable=true
quarkus.log.file.enable=true
quarkus.log.file.path=/tmp/jbh-personal-finance.log
```

### Profile-Based Configuration

- **dev/local**: Console + File logging with DEBUG level
- **test**: Console only with DEBUG level
- **prod/production**: Async File + JSON logging with INFO level

## Monitoring & Analysis

### Log Aggregation

The JSON output format is designed for easy integration with:

- ELK Stack (Elasticsearch, Logstash, Kibana)
- Splunk
- CloudWatch Logs
- Datadog

### Metrics & Alerts

Set up alerts based on:

- Error rate increase
- Missing tracking IDs
- Performance degradation (execution time in logs)

### Request Tracing

Use the `trackingId` to trace requests across:

- Multiple microservices
- Async operations
- Database transactions
- External API calls

This logging system provides comprehensive visibility into your application's behavior while maintaining high
performance and easy maintenance across all modules.