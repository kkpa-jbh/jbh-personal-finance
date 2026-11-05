# JBH Personal Finance - Logging Guide

## Overview

This guide explains how to use the centralized logging system across all modules in the JBH Personal Finance
application.

The logging system uses the **SLF4J API** on Quarkus (JBoss LogManager) with **MDC (Mapped Diagnostic Context)** support for tracking requests
across modules.

## Architecture

### Components

1. **SLF4J API**: Framework-agnostic logging facade
2. **JBoss LogManager**: The Quarkus implementation behind SLF4J (`logback-classic` is test scope only)
3. **MDC Context**: For tracking request metadata
4. **Quarkus logging configuration**: `application.properties` (JSON in prod, level per profile)
5. **Utility Classes**: For easy logging setup

### Module Structure

```
jbh-personal-finance/
├── pom.xml                    # Root POM with logging dependencies
├── jbh-finance/
│   ├── jbh-finance-domain/    # SLF4J API only
│   ├── jbh-finance-application/ # SLF4J API + Utility classes
│   └── jbh-finance-infra/     # Full logging implementation
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

### Logging Configuration

Quarkus runs on JBoss LogManager. There is no Logback configuration file. Everything is in
`jbh-z-assembly/src/main/resources/application.properties` (see "Configuration Options" below).
`logback-classic` is on the test classpath only. Code still logs through the SLF4J API.

**Key Features:**

- **MDC Support**: Tracks `trackingId`, `userId`, `productId`, `transactionId`, `module`
- **JSON in prod**: `quarkus-logging-json`, one JSON object per line. Dev and test use plain text.
- **Trace correlation**: `traceId` and `spanId` come from OpenTelemetry when the SDK is on
- **Level per profile**: `com.jbh` is DEBUG in dev and INFO in prod

## Utility Classes

### 1. LoggingContext

**Purpose**: Manage MDC values for request context tracking

**Location**: `logging.common.com.jbh.finances.application.LoggingContext`

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

**Location**: `logging.common.com.jbh.finances.application.LoggerFactory`

**Usage:**

```java
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

// Or structured logger
private static final StructuredLogger structuredLogger =
    LoggerFactory.getStructuredLogger(MyClass.class);
```

### 3. LoggingFilter

**Purpose**: Automatically set up logging context for HTTP requests

**Location**: `logging.common.com.jbh.finances.infra.LoggingFilter`

**Features:**

- Auto-generates `trackingId` and `requestId`
- Extracts `userId` from headers
- Logs request/response timing
- Does not log the client IP address (privacy rule)

## Usage Examples

### 1. Basic Logging in Domain Layer

```java
package com.jbh.financess_mgmt.accounts.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountDomain {

  private static final Logger logger = LoggerFactory.getLogger(AccountDomain.class);

  public void syncBalances(TransactionDomain transaction) {
    logger.debug("Syncing balance for product: {}, transaction: {}",
        getId().value(), transaction.getId().value());

    // Business logic

    logger.info("Balance updated successfully for product: {}", getId().value());
  }
}
```

### 2. Application Layer with Full Context

```java
package com.jbh.finances_app.accounts.ports.input;

import logging.common.com.jbh.finances.application.LoggerFactory;
import logging.common.com.jbh.finances.application.LoggingContext;

public class AddTransactionInputPort {

  private static final Logger logger = LoggerFactory.getLogger(AddTransactionInputPort.class);

  @Override
  public void addTransaction(AccountId accountId, AddTransactionWithDateAmount requestVO) {
    LoggingContext.builder()
        .accountId(accountId.value())
        .userId(requestVO.userId())
        .module("product-application")
        .execute(() -> {
          logger.info("Starting transaction addition for product: {}", accountId.value());

          try {
            // Business logic
            TransactionDomain txn = createTransaction(requestVO);
            LoggingContext.setTransactionId(txn.getId().value());

            logger.info("Transaction created successfully: {}", txn.getId().value());
          } catch (Exception e) {
            logger.error("Failed to add transaction for product: {}", accountId.value(), e);
            throw e;
          }
        });
  }
}
```

### 3. Infrastructure Layer with HTTP Context

```java
package com.jbh.finances_infra.controllers;

import logging.common.com.jbh.finances.application.LoggerFactory;
import logging.common.com.jbh.finances.application.LoggingContext;

@RestController
public class AccountController {

  private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

  @PostMapping("/products/{accountId}/transactions")
  public ResponseEntity<Void> addTransaction(
      @PathVariable UUID accountId,
      @RequestBody TransactionRequest request) {

    // Context already set by LoggingFilter, just add specific details
    LoggingContext.setModule("product-controller");

    logger.info("Received transaction request for product: {}", accountId);

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

Fields in the examples are illustrative. Ids are fake.

### Console Output (dev and test)

```
2025-01-15 14:30:15,123 INFO  [com.jbh.finance.infra.adapters.in.rest.product.ProductRestAdapter] (executor-thread-1) Received request for product: 4b1f0c1e
```

### JSON Output (prod)

```json
{
  "timestamp": "2025-01-15T14:30:15.123Z",
  "level": "INFO",
  "loggerName": "com.jbh.finance.infra.adapters.in.rest.product.ProductRestAdapter",
  "message": "Received request for product: 4b1f0c1e",
  "trackingId": "abc-123-def",
  "userId": "user-456",
  "module": "product-controller",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "sampled": "true"
}
```

MDC entries (`trackingId`, `userId`, `productId`, `transactionId`, `module`, and the OpenTelemetry
`traceId`, `spanId`, `sampled`) are top-level fields because of `quarkus.log.console.json.mdc.flat-fields=true`.
`traceId`, `spanId` and `sampled` appear only while the SDK is on and a span is active.

In dev the console is plain text. Each line prints `traceId=... spanId=...` (empty while the SDK is off):

```
2026-11-04 19:30:12,345 INFO  traceId=6273cd5f1fb6002a22538d67e9853981 spanId=a1b2c3d4e5f60718 [c.j.f.i.a.i.r.m.MovementRestAdapter] (executor-thread-1) Adding movement to Product 0b6f1c2e-5d4a-4f3b-9c1d-2e7a8b9c0d1e
```

Copy the `traceId` into Grafana → Explore → Tempo → TraceQL to open the request.
In dev, `%dev.quarkus.otel.logs.enabled=true` also sends the logs to the local LGTM stack (see below). Query them in
Explore → Loki with `{service_name="jbh-personal-finance"}`. Each record carries `trace_id`, with a link to Tempo.
Prod does not export logs with OpenTelemetry: it writes JSON to stdout, and Alloy collects it.

## Privacy Rule

Never put these in a log line (any level):

- amounts and balances
- statement text, OCR text
- LLM prompts or answers
- IBAN or account numbers
- e-mail addresses
- client IP address
- API keys
- transaction lists
- whole domain objects, DTOs or commands (`toString` can print any field)

Log ids, counts, periods, enum names and exception class names instead. A product name is free text
typed by the user, so log the product id.

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

| Variable | Default | Effect |
|----------|---------|--------|
| `OTEL_SDK_DISABLED` | `true` | Set to `false` to turn traces on. While `true`, nothing is exported and no exporter error appears. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | Prod profile only. OTLP gRPC endpoint of the collector (`http://jbh-alloy:4317` in prod). Dev always uses `http://localhost:4317` (the local LGTM stack). |
| `OTEL_TRACES_SAMPLER` | `parentbased_traceidratio` | Sampler. It honors the parent decision. |
| `OTEL_TRACES_SAMPLER_ARG` | `1.0` | Sampling ratio. `1.0` samples every trace. |

To see traces in prod, set both `OTEL_SDK_DISABLED=false` and `OTEL_EXPORTER_OTLP_ENDPOINT`.
In dev, traces and logs go to `http://localhost:4317` by default (`OTEL_SDK_DISABLED` defaults to `false` in dev).

### Local LGTM stack (Grafana, Tempo, Loki, Prometheus)

It runs in the `kkpa-docker-lab` repo (`~/cpena/sw/github/kkpa-docker-lab`), shared by all projects:

```bash
make lgtm up      # Grafana on http://localhost:3000 (admin / admin), data kept in a Docker volume
```

Its Prometheus pulls `host.docker.internal:7777/q/metrics`. While the stack is down, dev mode still
starts, and the console shows OTLP export warnings. To silence them, start dev with `OTEL_SDK_DISABLED=true`.

### Application Properties

All keys are in `jbh-z-assembly/src/main/resources/application.properties`, section `OBSERVABILITY`.

```properties
# com.jbh logs DEBUG in dev and INFO in prod. test uses the Quarkus default (INFO).
%dev.quarkus.log.category."com.jbh.**".level=DEBUG
%prod.quarkus.log.category."com.jbh.**".level=INFO
# JSON only in prod. MDC entries become top-level JSON fields.
quarkus.log.console.json.enabled=false
%prod.quarkus.log.console.json.enabled=true
quarkus.log.console.json.mdc.flat-fields=true
# Traces
quarkus.otel.sdk.disabled=${OTEL_SDK_DISABLED:true}
%dev.quarkus.otel.sdk.disabled=${OTEL_SDK_DISABLED:false}
%prod.quarkus.otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
%dev.quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
quarkus.otel.traces.sampler=${OTEL_TRACES_SAMPLER:parentbased_traceidratio}
quarkus.otel.traces.sampler.arg=${OTEL_TRACES_SAMPLER_ARG:1.0}
# Metrics
quarkus.datasource.metrics.enabled=true
jbh.metrics.env=prod
```

### Profile-Based Configuration

- **dev**: plain-text console, `com.jbh` at DEBUG, metric tag `env=dev`.
- **test**: plain-text console, default level (INFO), metric tag `env=test`.
- **prod**: JSON console, `com.jbh` at INFO, metric tag `env=prod`.

## Metrics

The app publishes Micrometer metrics (JVM, HTTP server, Agroal datasource) at `/q/metrics` on port 7777.
Every metric has the tags `service=jbh-personal-finance` and `env=<profile>`. HTTP metrics use the URI
template (`/jbh-api/products/{productId}`), so ids never become labels. The gateway routes only
`/jbh-api/**`, so `/q/metrics` is not public. Alloy scrapes it on the private network.

**Circuit breaker of the IAM calls.** `jbh-gateway-client` has a circuit breaker around `findUserId` and
`findUserGroup`. Both `GatewayClientBuilder` classes pass the `MeterRegistry`, so `/q/metrics` also has
`resilience4j_circuitbreaker_*` with `name=JBH_FINANCE_API.User` (finance) and `name=JBH_PREFERENCES_API.User`
(preferences). While the breaker is open, the call fails with error code `CIRCUIT_OPEN`.
Rules and metric list: `jbh-gateway-client/README.md`, "Circuit breaker". Dashboard: `jbh-deploy/observability/grafana/`.

## Traces

OpenTelemetry traces are off until `OTEL_SDK_DISABLED=false`. The four AI calls have their own spans:
`ai.azure.extract-text`, `ai.openrouter.answer`, `ai.gemini.answer`, `ai.jev.classify`. A span has only a
name, a duration, a status and the exception. It never has text, prompts or answers. `/q/*` is not traced.

## Monitoring & Analysis

Logs, traces and metrics go to Grafana (Loki, Tempo, Prometheus) through Alloy. To follow one request:

1. Take the `traceId` from a log line in Loki.
2. Open the same `traceId` in Tempo to see the spans.
3. Use `trackingId` to find all lines of one request, including other modules.

Metrics and traces are per instance. Grafana merges them. The app runs as 2 or more instances.