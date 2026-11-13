# Design: platform-add-observability

Source ticket: JBH-60. Owner: platform. Stack: Quarkus 3.33.4.1, Maven, Java 21.
Property names below come from the Quarkus 3.33 guides (logging, opentelemetry, telemetry-micrometer,
observability-devservices-lgtm), read with Context7. Items marked **VERIFY** could not be confirmed
in the 3 allowed doc queries. The worker confirms them (build or dev start) before it ticks the task.

## Decisions

### D1. Log level per profile
Replace line 15 of `jbh-z-assembly/src/main/resources/application.properties`:
```
%dev.quarkus.log.category."com.jbh.**".level=DEBUG
%prod.quarkus.log.category."com.jbh.**".level=INFO
```
There is no base line. `%test` falls back to the Quarkus default (INFO). Before, tests ran at DEBUG;
no test reads log levels. `io.quarkus.arc` stays INFO.

### D2. JSON logs in prod only
Dependency `io.quarkus:quarkus-logging-json` in `jbh-z-assembly`. With the extension on the classpath,
JSON is on by default, so the base value is `false` and only `%prod` turns it on:
```
quarkus.log.console.json.enabled=false
%prod.quarkus.log.console.json.enabled=true
quarkus.log.console.json.mdc.flat-fields=true
```
`flat-fields=true` writes MDC entries (`trackingId`, `userId`, `productId`, `transactionId`,
`module`, and the OTel `traceId`, `spanId`, `sampled`) as top-level JSON fields. Loki and Grafana
derived fields then read `traceId` directly. The default nested form (`mdc.traceId`) is rejected: it is harder to query.
`LoggingContext` writes MDC through `org.slf4j.MDC`. Quarkus routes SLF4J to JBoss LogManager, so the
MDC values reach the JSON handler. No code change. `logback-classic` is test scope only.
Because `json.enabled=false` is the base value, `dev` and `test` need no own line.

### D3. Traces and the "off until asked" switch
Dependency `io.quarkus:quarkus-opentelemetry` in `jbh-z-assembly`.
```
quarkus.application.name=jbh-personal-finance
quarkus.otel.sdk.disabled=${OTEL_SDK_DISABLED:true}
%prod.quarkus.otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
quarkus.otel.traces.sampler=${OTEL_TRACES_SAMPLER:parentbased_traceidratio}
quarkus.otel.traces.sampler.arg=${OTEL_TRACES_SAMPLER_ARG:1.0}
```
- `quarkus.otel.sdk.disabled` is a runtime property (documented). With the SDK disabled nothing is
  exported, so no exporter error can appear. This is the user's rule: prod has no exporter
  errors before Alloy exists.
- Ratio 1.0 is 100% sampling. `parentbased_traceidratio` honors the parent decision and the env
  var can lower the ratio later without a code change.
- Prod switch-on (JBH-56, in `jbh-deploy`): `OTEL_SDK_DISABLED=false` and
  `OTEL_EXPORTER_OTLP_ENDPOINT=http://jbh-alloy:4317`. These are 2 env vars, not 1. The reason: the
  property system cannot turn "endpoint is set" into "SDK is on". Rejected: a custom config class
  that computes it. Not worth the code.
- `traceId`, `spanId` and `sampled` are put in the MDC by the extension, so they appear in the JSON
  logs with D2. When the SDK is disabled there are no spans, so these fields are absent. This is expected.
- `/q/*` URIs are not traced (default `quarkus.otel.traces.suppress-non-application-uris=true`).
- The endpoint key is `%prod` only (fix F1, user decision D22). The LGTM Dev Service sets the endpoint
  and the protocol in dev, but an explicit key in `application.properties` wins over it. With a base
  key, dev sent traces to `localhost:4317`, where nothing listens, and Tempo stayed empty.
- Dev console format prints `traceId` and `spanId`, and dev exports logs with OpenTelemetry
  (`%dev.quarkus.otel.logs.enabled=true`, build time, tech preview) so Loki in the LGTM stack links
  logs to traces (fix F2, user decision D23). Prod keeps JSON on stdout for Alloy; no OTel log export.
- Exporter protocol: gRPC on 4317 (the default). Alloy receives OTLP on `jbh-private`.
- Trace attributes: the HTTP server span has the raw `url.path`. Span attributes are not metric labels, so
  the cardinality rule does not apply. The query string is not recorded by the extension.

### D4. Metrics: Micrometer plus Prometheus registry
Dependency `io.quarkus:quarkus-micrometer-registry-prometheus` in `jbh-z-assembly` (it brings
`quarkus-micrometer`). Endpoint: `/q/metrics` on the main port 7777. Alloy scrapes it on `jbh-private`.
```
quarkus.datasource.metrics.enabled=true
```
Reason for Micrometer: the Quarkus 3.33 guides mark OTel metrics (`quarkus.otel.metrics.enabled`)
and `quarkus-micrometer-opentelemetry` as preview. Micrometer is stable and gives JVM, HTTP server and
Agroal datasource metrics. `quarkus.datasource.metrics.enabled=true` is **VERIFY** (Agroal metrics are
not on by default; the exact key is from memory).
- HTTP server metrics use the URI template (`/jbh-api/products/{productId}`), so a product id never
  becomes a label. Unmatched paths are reported as `NOT_FOUND`. If a raw id still shows in a test,
  add `quarkus.micrometer.binder.http-server.match-patterns` or `ignore-patterns` (documented keys).
- `/q/metrics` is not public: the gateway routes only `/jbh-api/**`. **VERIFY** that the app has no
  auth policy on `/q/*` that blocks the scrape (health at `/q/health` already works without a token).

### D5. Common tags `service` and `env`
The Micrometer guide has **no config property** for common tags. It documents a CDI `MeterFilter`
producer. So one small class is needed (config preferred, but impossible):
`com.jbh.assembly.config.MetricsTagsConfig` in `jbh-z-assembly`.
```java
@Singleton
public class MetricsTagsConfig {
  public MetricsTagsConfig(
      @ConfigProperty(name = "quarkus.application.name") String service,
      @ConfigProperty(name = "jbh.metrics.env") String env) { ... }

  @Produces @Singleton
  public MeterFilter commonTags() { return MeterFilter.commonTags(Tags.of("service", service, "env", env)); }
}
```
Constructor injection of both values makes it testable without CDI.
Config in `application.properties`:
```
jbh.metrics.env=prod
%dev.jbh.metrics.env=dev
%test.jbh.metrics.env=test
```
`env=prod` is the base value (ticket). Dev and test runs show their own value so they never mix with prod.
JPMS: `jbh-z-assembly` has a `module-info.java`; add `requires micrometer.core;` (**VERIFY** the
automatic module name) and `requires jakarta.inject;` is already present.

### D6. AI spans with `@WithSpan`
Annotation `io.opentelemetry.instrumentation.annotations.WithSpan` on the public port method of each
adapter in `jbh-finance-infra` (infra only; application and domain get no OTel type):

| Adapter | Method | Span name |
|---|---|---|
| `AzureLayoutTextSourceAdapter` | `extractText(byte[])` | `ai.azure.extract-text` |
| `OpenRouterLlmAdapter` | `answer(String)` | `ai.openrouter.answer` |
| `GeminiLlmAdapter` | `answer(String)` | `ai.gemini.answer` |
| `JevMovementClassifierAdapter` | `classify(List)` | `ai.jev.classify` |

- No `@SpanAttribute`. The span carries only name, duration, status and the exception. No text, prompt or answer.
- A thrown exception sets the span status to error and records the exception. The adapters throw
  `BusinessException` on failure, so a failed call is an error span. Retries inside the method (Azure
  429, Jev retry) stay inside one span: one span per provider call from the caller's view.
- **Self-invocation.** `OcrLlmStatementReaderAdapter` calls `llm.extract(text)`. `extract` is a default
  method of `StatementLlm` that calls `this.answer(...)`. Weld does not intercept self-calls. ArC
  (Quarkus) does: the Quarkus CDI reference describes "self-interception" for subclass-based
  interception (**VERIFY**, from memory; the doc quota was used). So `@WithSpan` goes on `answer`,
  and the span fires. Fallback if it does not fire: `OcrLlmStatementReaderAdapter` calls `answer` and
  `StatementExtractionFormat.parse` itself. Do not do this unless the test shows no span.
- `Azure.extractText` and `Jev.classify` are called from other classes, so they are plain proxy calls.
- `llmcompare/` (`OllamaLlmAdapter`, `ComparingStatementLlm`) is the dev-only comparison
  (`jbh.statement.compare`). Not annotated. Only `OpenRouterLlmAdapter` and `GeminiLlmAdapter` are prod
  providers (`jbh.statement.llm`).
- Dependencies in `jbh-finance-infra/pom.xml`: `io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations`
  (managed by the Quarkus BOM) and `io.opentelemetry:opentelemetry-api`. Add them with no version.
  JPMS: add the matching `requires` to `jbh-finance-infra/.../module-info.java` (**VERIFY** the
  automatic module names, expected `io.opentelemetry.instrumentation.annotations` and `io.opentelemetry.api`;
  `io.opentelemetry.context` is needed for D7). The extension itself stays only in `jbh-z-assembly`.

### D7. Pass the OTel context to the import thread
`StatementImportDispatcher.dispatch` runs the import on a virtual-thread executor. A new thread has no
OTel context, so the 4 AI spans would be new root traces, not children of the request. Fix, one line:
`executor.submit(Context.current().wrap(() -> runner.run(command)))` with
`io.opentelemetry.context.Context`. The HTTP span of `POST /statement-imports` has ended when the
import runs. A child of an ended span is valid in OTel. MDC (`trackingId`) is not copied to the
thread today. That is an existing behavior and stays out of scope.
User-confirmed: the extra file is accepted. It is needed for acceptance G5.

### D8. Local dev with LGTM, off by default
**Replaced by F3 (user decision D24):** the LGTM Dev Service is removed. The stack runs in
`kkpa-docker-lab` (`lgtm/`, fixed ports 3000/4317/4318/9090, data in a Docker volume, Prometheus job for
`host.docker.internal:7777`). Dev exports by default: `%dev.quarkus.otel.sdk.disabled=${OTEL_SDK_DISABLED:false}`
and `%dev.quarkus.otel.exporter.otlp.endpoint=http://localhost:4317`. Dev without Docker still starts; the
console shows OTLP export warnings. The text below is the original decision.

Dependency `io.quarkus:quarkus-observability-devservices-lgtm`, scope `provided` (dev and test only;
not in the prod package), in `jbh-z-assembly`.
```
quarkus.observability.enabled=false
```
(**VERIFY** the key against the observability-devservices-lgtm guide before the task is ticked. If it
differs, use the documented "disable Dev Services" key.) Turn on: `QUARKUS_OBSERVABILITY_ENABLED=true`
(env var overrides the file) and `OTEL_SDK_DISABLED=false`. That is 2 env vars, not 1: same reason as D3. Dev without Docker
works as today, because the stack is off. `@QuarkusTest` never starts it (off in every profile).

### D9. `logback-spring.xml`
Delete `jbh-finance-infra/src/main/resources/logback-spring.xml`. Reasons: Spring name, not read by
Quarkus (JBoss LogManager, `logback-classic` is test scope only); it names a Logstash encoder that is not
on the classpath; it misleads readers into thinking JSON logs exist already. The only reference is
`docs/guides/logging.md`; the task rewrites that guide.

### D10. Capability: new `platform-observability`, not a delta
`platform-quarkus-runtime` holds the upgrade rules: supported version, valid config keys, "behavior
unchanged". Observability is a separate concern with its own lifecycle (Alloy, Grafana). A delta would
mix two reasons to change in one spec. New capability. The upgrade spec stays valid: the
"Valid configuration" requirement still holds because every new key is valid in 3.33.

### D11. SRP only (user decision)
OCP and DIP are not needed. See `Responsibilities`.

### D12. Log audit
User decision: all INFO/WARN/ERROR calls in all modules (234 calls in 69 files read; `*.info/warn/error`
by `log`, `LOG`, `LOGGER`, `logger`). Most calls are safe (ids, counts, periods, enum names, exception
class names). The AI adapters and the statement use cases already log counts and class names only. The
list below is every line to change. Rules for the replacement: ids, counts, periods, enum names, class names.
Product names are free text typed by the user (could hold an account number), so they are replaced by the product id.
Paths: `APP` = `jbh-finance/jbh-finance-application/src/main/java/com/jbh/finance/application`,
`DOM` = `jbh-finance/jbh-finance-domain/src/main/java/com/jbh/finance/domain`,
`NOT` = `jbh-notification/jbh-notification-infra/src/main/java/com/jbh/notification/infra`,
`INFRA` = `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra`.

| File:line | What leaks | Replacement |
|---|---|---|
| `APP/async/AsyncTaskExecutorImpl.java:63, :71` | whole `asyncTask` (`toString` prints the free `metadata` map) | `asyncTask.type()` |
| `APP/async/AsyncTaskExecutorImpl.java:127` | `asyncTask.metadata()` | drop the metadata argument; keep `type()` and the message |
| `APP/feature/monthlybalance/services/ProcessMonthlyBalanceServiceImpl.java:284` | whole `asyncTask` | `asyncTask.type()` |
| `APP/feature/product/ports/input/EditProductInputPort.java:55` | product name | product id and user id |
| `APP/feature/product/ports/input/UpdateProductStatusInputPort.java:36, :50` | product name | product id and user id |
| `APP/feature/product/ports/input/DeleteProductInputPort.java:40` | product name | product id and user id |
| `APP/feature/product/ports/input/CreateProductInputPort.java:49` | product name | product id (from the created DTO), type, user id |
| `APP/feature/product/services/ProductLifecycleServiceImpl.java:92` | net profit and balance | product id only |
| `APP/feature/product/services/ProductLifecycleServiceImpl.java:115` | balance | product id only |
| `APP/feature/product/services/ProductLifecycleServiceImpl.java:167` | product name and whole `movement` (amount, description) | product id and movement id |
| `APP/feature/product/services/ProductLifecycleServiceImpl.java:197` | whole `movement` (domain object) | movement id |
| `APP/feature/movement/ports/input/LiquidateProductInputPort.java:74` | whole `toInternalAccount` (name, balance) | `toInternalAccount.id().value()` |
| `APP/feature/movement/ports/input/AddTransferJbhProductsInputPort.java:56` | product names and transfer amount | from and to product ids |
| `APP/feature/movement/ports/input/AddTransferJbhProductsInputPort.java:90, :94` | product names | product ids |
| `APP/feature/movement/services/ProcessMovementServiceImpl.java:89` | profit amount and balance snapshot | product id and period |
| `APP/feature/movement/services/ProcessMovementServiceImpl.java:121` | whole `movementCommand` (amount, description) | drop the last argument (type and product id stay) |
| `APP/feature/movement/services/ProcessMovementServiceImpl.java:163, :182` | dividends and tax amounts | product id and period |
| `APP/feature/movement/services/ProcessMovementServiceImpl.java:278` | product name | product id |
| `APP/feature/movement/services/ProcessMovementServiceImpl.java:281` | whole `movementDTO` | movement id (or drop the argument if the DTO has no id yet) |
| `DOM/monthlybalance/service/MoneyGrowthCalculator.java:58` | opening, closing, movement balance, growth | text without values: "Net growth is above the maximum" |
| `DOM/monthlybalance/MonthlyBalanceDomain.java:159` | opening balance | period only |
| `DOM/product/ProductDomain.java:299` | `movementAmount` | drop the argument; keep the exception `e` |
| `INFRA/common/logging/LoggingFilter.java:47` | client IP address (`getClientIP`) | drop the IP argument and "from IP: {}"; keep method and path |
| `NOT/adapters/out/email/QuarkusMailerAdapter.java:35, :52` | recipient address and subject | no argument ("Sending async email", "Sending sync email") |
| `NOT/adapters/out/email/QuarkusMailerAdapter.java:69` | recipient address | no argument |
| `NOT/adapters/out/email/QuarkusMailerAdapter.java:75` | recipient address and `error.getMessage()` (SMTP messages can hold the address) | `error.getClass().getSimpleName()` |
| `NOT/service/NotificationService.java:67, :123` | `recipientEmail()` | notification id (`notification.id()`) |
| `NOT/service/NotificationService.java:148` | `failure.getMessage()` (can hold the address) | `failure.getClass().getSimpleName()` |
| `NOT/config/MailerConfigLogger.java:56, :58` | sender address; masked user name (shows first characters and the domain) | "set" or "not set" |

Checked and kept: all `StatementImport*`, `ExtractStatementMovementsInputPort`, AI adapter lines (counts,
HTTP status, error code, class names; `describe(totals)` is currency plus status), REST adapter lines
(ids, `monthsBack`, periods; `MonthlyBalanceRequest` is two `YearMonth` values), exception handlers
(`BusinessException` messages are constants, spot-checked), `LoggingFilter:100` (method, raw path, status, duration; the IP of line 47 is removed, see the table), Consul and bootstrap lines, `ExcelMovementReaderService` sheet name.
Out of scope: DEBUG lines, test code.
Two lines have a leak risk that is not visible in the code: `AsyncTask.toString` (free map) and
`PreferencesLookupAdapter:45, :70` (`e.getMessage()` of the gateway client). The second stays: it carries
ids only today.
Edit rules for the audit: change only the arguments and the matching `{}` of the message. Keep the level
and the sentence. Do not add new log lines. Existing tests must not change.

## Responsibilities

| Class / file | One responsibility |
|---|---|
| `jbh-z-assembly/pom.xml` | Declares the observability dependencies. |
| `jbh-z-assembly/.../application.properties` | Holds the per-profile config of logs, traces, metrics and dev stack. |
| `MetricsTagsConfig` (new, `jbh-z-assembly`) | Produces the `MeterFilter` that adds the common tags `service` and `env`. Needed only because Quarkus has no property for it. |
| 4 AI adapters | No new job. They only get `@WithSpan` on the port method. |
| `StatementImportDispatcher` | Job unchanged: starts the import on a background thread. It only passes the OTel context to that thread (D7, user-confirmed). |
| Audit files (17, with `LoggingFilter`) | No new job. Only log arguments change. |

OCP and DIP: not needed (no second implementation, no module boundary, no test seam).

## Test contract (Test-First)

### Unit tests (offline, `jbh-z-assembly`)
`jbh-z-assembly` is excluded from the coverage rule, but both tests are cheap and offline, so they are
written first (blueprint). Folder: `jbh-z-assembly/src/test/java/com/jbh/assembly/config/`. Names follow
`<ClassName>Test` and `should<Expected>When<Condition>`.

`MetricsTagsConfigTest` (entry point `new MetricsTagsConfig(service, env).commonTags()`, then
a `SimpleMeterRegistry` with `registry.config().meterFilter(filter)`):

| Test | Input | Expected |
|---|---|---|
| `shouldAddServiceAndEnvTagsWhenMeterIsCreated` | service `jbh-personal-finance`, env `prod`; `registry.counter("jbh.test")` | tag `service` = `jbh-personal-finance`, tag `env` = `prod` |
| `shouldUseGivenEnvWhenEnvIsDev` | service `jbh-personal-finance`, env `dev` | tag `env` = `dev` |
| `shouldAddOnlyTwoTagsWhenMeterHasNoOwnTags` | same as the first | the meter id has exactly 2 tags (`service`, `env`): no user id, no product id |
| `shouldKeepOwnTagsWhenMeterHasThem` | `registry.counter("jbh.test", "result", "ok")` | tags `result`=`ok`, `service`, `env` (3 tags) |

`ObservabilityConfigPropertiesTest` (entry point: `java.util.Properties` loaded from the file
`src/main/resources/application.properties` by relative path; not the classpath, because other
modules also ship an `application.properties`). It guards the static acceptance rules G1 to G3:

| Test | Expected value |
|---|---|
| `shouldLogInfoInProdAndDebugInDevWhenCategoryIsComJbh` | `%prod.quarkus.log.category."com.jbh.**".level` = `INFO`; `%dev...` = `DEBUG` |
| `shouldHaveNoProfilelessLevelWhenCategoryIsComJbh` | key `quarkus.log.category."com.jbh.**".level` is absent |
| `shouldEnableJsonOnlyInProdWhenLoggingConsole` | `quarkus.log.console.json.enabled` = `false`; `%prod.quarkus.log.console.json.enabled` = `true` |
| `shouldWriteMdcAsTopLevelFieldsWhenJsonIsOn` | `quarkus.log.console.json.mdc.flat-fields` = `true` |
| `shouldDisableOtelSdkByDefaultWhenNoEnvVar` | `quarkus.otel.sdk.disabled` = `${OTEL_SDK_DISABLED:true}` |
| `shouldSampleAllByDefaultWhenNoSamplerEnvVar` | `quarkus.otel.traces.sampler.arg` = `${OTEL_TRACES_SAMPLER_ARG:1.0}` |
| `shouldReadEndpointFromEnvWhenConfigured` | `%prod.quarkus.otel.exporter.otlp.endpoint` starts with `${OTEL_EXPORTER_OTLP_ENDPOINT:` (key changed by F1, D22) |
| `shouldLeaveBaseEndpointUnset` | `quarkus.otel.exporter.otlp.endpoint` is absent (added by F1, renamed by F3) |
| `shouldNotConfigureLgtmDevService` | `quarkus.observability.enabled` is absent (replaces `shouldKeepLgtmOffWhenNoEnvVar`, F3, D24) |
| `shouldExportByDefaultInDevToLocalStack` | `%dev.quarkus.otel.sdk.disabled` is `${OTEL_SDK_DISABLED:false}`; `%dev` endpoint is `http://localhost:4317` (added by F3) |
| `shouldPrintTraceAndSpanIdsInDevConsole` | `%dev.quarkus.log.console.format` contains `traceId=%X{traceId}` and `spanId=%X{spanId}` (added by F2) |
| `shouldExportLogsToOtelOnlyInDev` | `%dev.quarkus.otel.logs.enabled` is `true`; base and `%prod` keys absent (added by F2) |
| `shouldKeepLgtmOffWhenNoEnvVar` | `quarkus.observability.enabled` = `false` |
| `shouldEnableDatasourceMetrics` | `quarkus.datasource.metrics.enabled` = `true` |
| `shouldSetEnvPerProfileWhenTagging` | `jbh.metrics.env` = `prod`; `%dev.jbh.metrics.env` = `dev`; `%test.jbh.metrics.env` = `test` |

The log audit and the `@WithSpan` annotations have no unit-testable logic. They are checked by the
review (G6: grep of the audit list) and by the integration checks below.
The red check: `MetricsTagsConfigTest` fails on the skeleton (throws "not implemented") and
`ObservabilityConfigPropertiesTest` fails because the keys are missing. Both fail for the right
reason. Existing tests are not touched.

### Integration checks (run by the user or leader, against the contract; no new `@QuarkusTest`)
Run the built jar: `QUARKUS_PROFILE=prod java -jar jbh-z-assembly/target/quarkus-app/quarkus-run.jar` with a local PostgreSQL.

| # | Input | Expected |
|---|---|---|
| I1 | start with `prod`, no OTel env var; read the first 50 console lines | every line starts with `{` and is valid JSON; it has `timestamp`, `level`, `loggerName`, `message`; no line with `"level":"DEBUG"` for a `com.jbh` logger; no text with `OTLP` or `Failed to export` |
| I2 | `OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317` with a local collector; `GET /q/health` is skipped (not traced); call `POST /jbh-api/...` with a valid token | the line written during the request has top-level `traceId` (32 hex chars), `spanId` (16 hex chars), `trackingId`, `userId`; the same `traceId` is in the collector |
| I3 | `GET http://localhost:7777/q/metrics` | contains `jvm_memory_used_bytes{...,env="prod",service="jbh-personal-finance"...}`, a line starting `http_server_requests_seconds_count` and a line starting `agroal_`; every sample line has both `service="jbh-personal-finance"` and `env="prod"` |
| I4 | call a product endpoint with 3 product ids, then read `/q/metrics` | `uri` label is the template (for example `/jbh-api/products/{productId}`); `grep -c "<a raw product uuid>"` is `0`; no label named `userId` or `productId` |
| I5 | dev with `QUARKUS_OBSERVABILITY_ENABLED=true OTEL_SDK_DISABLED=false` and Docker; import a statement through the REST flow | Grafana (Tempo) shows one trace for the import with the spans `ai.azure.extract-text`, `ai.openrouter.answer` (or `ai.gemini.answer`), `ai.jev.classify`, each with a duration, all in the same trace as the `POST` that started the import |
| I6 | AI provider with a wrong key | `ai.openrouter.answer` span status is error; the span has an `exception.type` attribute and no response text |
| I7 | dev with no env var and no Docker | `quarkus:dev` starts; no container starts; log lines are plain text with DEBUG |
| I8 | `GET /q/health` and the Consul UI | `UP`; the service has the same name and tags as before |
| I9 | grep audit: `grep -rnE "\b(log\|LOG\|LOGGER\|logger)\.(info\|warn\|error)\(" --include=*.java` and read each line of the D12 table | no argument named `*Amount`, `*Balance`, `movement`, `movementDTO`, `movementCommand`, `asyncTask`, `recipient*`, `emailRequest.to()`, `getClientIP`, or a product `.name()` |

## Concurrency

none. The change is stateless. 2 or more instances: each instance runs its own SDK, batch span processor and Micrometer
registry in memory. This is allowed because they only carry telemetry, never business data. Two
instances handling the same product at the same time are protected by the existing `lockForUpdate`;
this change adds no shared data and no new lock. Metrics of the instances are told apart by the
scraper (the `instance` label that Alloy adds), not by the app. `MetricsTagsConfig` has final fields
only. The context wrap in D7 copies an immutable `Context` to one thread; no shared mutable state.

## Horizontal scaling

The app runs as 2 or more instances. Nothing here uses in-JVM state for correctness. Traces, logs
and metrics are per instance and are merged by Grafana. A restart loses only the spans and counters not yet exported.

## Database

No database change. No table, column, index, constraint, view or migration.

## Risks and open items for the leader

- **VERIFY** list: `quarkus.datasource.metrics.enabled`; `quarkus.observability.enabled`; ArC
  self-interception for `StatementLlm.answer`; JPMS automatic module names (`micrometer.core`,
  `io.opentelemetry.*`); `/q/metrics` not blocked by an auth policy.
- Prod needs 2 env vars to turn tracing on (D3). The ticket says "an env var" for the endpoint. The second one is the SDK switch.
- JPMS: both `jbh-z-assembly` and `jbh-finance-infra` have `module-info.java`. A missing `requires`
  is a compile error found by the leader's build, not a runtime risk.
- The prod JSON log changes the shape of `docker logs`. Nothing in the repo parses it.
