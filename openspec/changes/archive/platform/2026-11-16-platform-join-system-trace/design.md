# Design: platform-join-system-trace

Source ticket: JBH-81 (part of JBH-55, section 5).
Reference design: `jbh-notification` change `jbh-notification-add-otel-export-histograms`
(`../jbh-notification/openspec/changes/jbh-notification-add-otel-export-histograms/design.md`).
D8 is reused as is. D1, D2, D4 and D7 follow the same shape.

## Context

Quarkus app (`jbh-z-assembly` is the bootstrap module). Micrometer (Prometheus registry) and
`quarkus-opentelemetry` are already there. No Maven dependency is added. The collector is Alloy:
`http://jbh-alloy:4317` in Docker, `http://localhost:14317` in the IDE.
`jbh-gateway-client` 1.0.0-SNAPSHOT (with `JbhGatewayClientBuilder.openTelemetry(...)`) is already
installed in `~/.m2`. The client only uses the OpenTelemetry API and never reads
`GlobalOpenTelemetry`, so we must pass the instance. Quarkus gives it as the CDI bean
`io.opentelemetry.api.OpenTelemetry`, with the W3C propagator already set.

Facts checked in the code (CodeGraph and grep):
- `LoggingContext` is used by `LoggingFilter` only (no other caller in main or test code).
- No test reads `trackingId`, `requestId`, `LoggingFilter` or `LoggingContext`.
- Four adapters have `@WithSpan`: `AzureLayoutTextSourceAdapter`, `OpenRouterLlmAdapter`,
  `GeminiLlmAdapter`, `JevMovementClassifierAdapter`. None uses `@SpanAttribute`, `setAttribute`
  or `Span.current()`.

## Responsibilities

- `MetricsTagsConfig` (existing, gets a second job): Micrometer meter filters for this app (common
  tags + HTTP server latency buckets). Changes when: metric policy changes. No new class.
- `application.properties` (`jbh-z-assembly`, existing): app config and env vars. Changes when:
  config or env vars change.
- `LoggingFilter` (existing, loses jobs): request and response log lines, and the MDC user id and
  module. Changes when: request logging changes.
- `LoggingContext` (existing, loses jobs): the MDC keys the app writes. Changes when: the MDC keys
  change.
- `GatewayClientBuilder` (existing, x2, finance and preferences): builds the gateway client, now
  with OpenTelemetry. Changes when: the client setup changes.
- `ObservabilityConfigPropertiesTest` (existing): guards the property values. Changes when: those
  values change.
- `deployment.md`, `docs/guides/logging.md` (existing): docs.
- New classes: none. SOLID: SRP only. No new interface and no extension point (user decision).

## Decisions

### D1. Properties in the OBSERVABILITY block (jbh-z-assembly)
Final state of the changed lines (the rest of the block stays):

```properties
# MDC entries (userId, productId, traceId, spanId, ...) become top-level JSON fields.
quarkus.log.console.json.mdc.flat-fields=true
# Dev console is plain text, so print the trace ids to copy them into Tempo. Empty only without an incoming or own trace.
%dev.quarkus.log.console.format=<unchanged>
# OTLP log exporter is built in every profile; the export switch decides at run time if it sends.
quarkus.otel.logs.enabled=true
# (export switch lines: see D3)
# One OTLP gRPC endpoint for all profiles: Alloy in Docker, the Alloy dev port in the IDE.
quarkus.otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14317}
quarkus.otel.resource.attributes=service.namespace=jbh,deployment.environment.name=${jbh.metrics.env},service.version=${JBH_SERVICE_VERSION:unknown}
jbh.metrics.env=${JBH_ENV:prod}
%dev.jbh.metrics.env=dev
%test.jbh.metrics.env=test
```

- Removed: `%dev.quarkus.otel.logs.enabled`, `%prod.quarkus.otel.exporter.otlp.endpoint`,
  `%dev.quarkus.otel.exporter.otlp.endpoint`, `quarkus.otel.sdk.disabled=${OTEL_SDK_DISABLED:true}`,
  `%dev.quarkus.otel.sdk.disabled`, and the old `quarkus.otel.traces.sampler` line (D3 replaces it).
- `quarkus.otel.logs.enabled` is a build-time property. It only builds the log exporter. Whether
  logs go out is decided at run time by D3. Export is OFF by default, also in prod, until JBH-84.
- `%dev` and `%test` overrides of `jbh.metrics.env` stay, so dev and test never mix with prod.
  In dev, `JBH_ENV` is ignored (the `%dev` line wins). Same as today.
- The comment that lists MDC entries drops `trackingId`.

### D2. One source for the environment name
`deployment.environment.name` reads `${jbh.metrics.env}`, so the metric tag `env` and the resource
attribute always match, and `JBH_ENV` is read in one place.

### D3. Run-time export switch (reuse of notification D8)
Same keys and values as notification D8. Config only, no Java. `OTEL_SDK_DISABLED` stops only the
OTLP export. Tracing stays on in every profile.

```properties
quarkus.otel.sdk.disabled=false
jbh.otel.export-off=${OTEL_SDK_DISABLED:true}
%dev.jbh.otel.export-off=${OTEL_SDK_DISABLED:false}
jbh.otel.sampler.true=always_off
jbh.otel.sampler.false=${OTEL_TRACES_SAMPLER:parentbased_traceidratio}
jbh.otel.logs-handler.true=false
jbh.otel.logs-handler.false=true
quarkus.otel.traces.sampler=${jbh.otel.sampler.${jbh.otel.export-off}:always_off}
quarkus.otel.logs.handler.enabled=${jbh.otel.logs-handler.${jbh.otel.export-off}:false}
```

`quarkus.otel.traces.sampler.arg=${OTEL_TRACES_SAMPLER_ARG:1.0}` stays. A short plain-English
comment above the lines says what the switch does, why it is a lookup (the exporter properties
are build time), and that a bad value falls back to off.

How it works: `jbh.otel.export-off` is the only line that reads `OTEL_SDK_DISABLED`. Default
`true`; `%dev` makes the default `false`. `%test` has no override, so export is off in tests while
the SDK is on. Quarkus resolves `${a.${b}}` inner first, so the key `jbh.otel.sampler.<true|false>`
is chosen at run time. OFF: sampler `always_off`, logs handler off, nothing is sent, and the
exporter objects stay idle. ON: sampler is `OTEL_TRACES_SAMPLER` (default
`parentbased_traceidratio`) and the logs handler is on. Quarkus puts `traceId` and `spanId` in the
MDC for every span, sampled or not, so logs keep the trace id with export off. A bad value (`1`,
`TRUE`) finds no lookup key, so the defaults `always_off` and `false` apply: export off, service
starts. Values are case-sensitive `true` and `false`.

Known limit: with export off the outgoing `traceparent` has sampled=0. Finance calls `jbh-iam`
through the gateway. If `jbh-iam` has export on and finance has export off, the parent-based
sampler in `jbh-iam` drops its spans for that request. This matters only in a mixed setup. In prod
all services switch together (JBH-84).

Rejected options: `quarkus.otel.sdk.disabled` as the switch (turns ALL OTel off, so no trace ids);
`quarkus.otel.exporter.otlp.enabled`, `quarkus.otel.traces.exporter` (build time, an env var in a
prod image cannot change them); an empty endpoint (Quarkus falls back to `http://localhost:4317/`);
a Java `Sampler` bean (more code, the logs handler still needs its own logic); sampler only (the
logs handler would still export). New config names: the `jbh.otel.*` keys are ours.

### D4. Latency buckets as a second MeterFilter
`MetricsTagsConfig` gets a second `@Produces @Singleton` method `httpServerLatency()`:

- It overrides `MeterFilter.configure(Meter.Id id, DistributionStatisticConfig config)`.
- If `id.getName()` equals `http.server.requests` (exact match), it returns
  `DistributionStatisticConfig.builder().serviceLevelObjectives(<nanos as double>...).build().merge(config)`.
  Otherwise it returns `config` unchanged.
- Buckets (11): 5 ms, 10 ms, 25 ms, 50 ms, 100 ms, 250 ms, 500 ms, 1 s, 2.5 s, 5 s, 10 s. Values
  are nanoseconds for timers, built with `Duration.ofMillis(x).toNanos()`, kept in one
  `private static final` array. The metric name is a named constant (no literal in the `if`).
- The constructor and `commonTags()` do not change, so `MetricsTagsConfigTest` stays valid. The
  class Javadoc becomes: Micrometer meter filters for this app.
- Rejected: a new class (user decision, SRP line is "meter filters for this app");
  `publishPercentileHistogram` (many more series); a Quarkus property (none exists for SLOs).
- Series cost: 11 buckets + `+Inf` per tag set of `http.server.requests`. Other meters untouched.
- Complexity: one `if`, cyclomatic 2 for `configure`. Well under the limits.

### D5. Remove trackingId and requestId
- `LoggingFilter`: remove `TRACKING_ID_HEADER`, `REQUEST_ID_HEADER`, `extractTrackingId`,
  `extractRequestId`, the two `setProperty` calls, the response headers, and the `UUID` import.
  The request filter keeps `userId` (header `X-User-ID`) and `module`. The response filter keeps the
  duration log and `LoggingContext.clear()`. With the two properties gone, the response filter
  reads only the start time.
- `LoggingContext`: remove `TRACKING_ID`, `REQUEST_ID`, `getRequestId`, both `setRequestId`, `withTrackingId` (2
  overloads), `getTrackingId`, both `setTrackingId`, the builder fields `trackingId` and
  `requestId`, their four builder methods, and their two branches in `apply()`. Safe: CodeGraph and
  grep show no other caller. `userId`, `productId`, `movementId`, `module` and `clear()` stay.
  Nothing else in the class changes.
- The `UUID` and `Supplier` imports stay only if still used (the compiler will tell: the
  leader's build).
- `X-User-ID` stays. It is not a trace id.
- Logs already have `traceId` and `spanId` from OpenTelemetry. No replacement is added.

### D6. Join the trace across services
Both `GatewayClientBuilder` classes (finance and preferences) take
`io.opentelemetry.api.OpenTelemetry` in the `@Inject` constructor, keep it in a `final` field, and
call `.openTelemetry(openTelemetry)` on `JbhGatewayClientBuilder` before `.build()`. A short
comment says why: without it the client makes no span and sends no `traceparent`. The constructor
has 3 parameters (the limit). `jbh-finance-infra` already has `opentelemetry-api` in its pom.
`jbh-preferences-infra` has no OpenTelemetry dependency in its pom. `jbh-gateway-client` declares
`opentelemetry-api`; if its scope is compile, the type comes through it and nothing is added.
If it does not (the build fails to find the type), the fix is the same dependency finance has
(`io.opentelemetry:opentelemetry-api`, version managed by the Quarkus BOM). That is a new
dependency, so the worker reports task 4.2 `blocked` and names it, and the leader asks the user.
The dependency would be justified because the constructor must name the CDI bean type, and no
existing dependency gives it.

### D7. Sensitive data in telemetry (findings)
- The four `@WithSpan` methods set only the span name (`ai.azure.extract-text`,
  `ai.openrouter.answer`, `ai.gemini.answer`, `ai.jev.classify`). No `@SpanAttribute`, no
  `setAttribute`, no `Span.current()`. So spans carry no statement content, amount, description
  or e-mail from our code. Auto spans are HTTP server spans (method, route, status, `url.path`).
- The gateway client span name is `<METHOD> <url.template>`. The client README says no id, query
  string, header or body goes into the span.
- The task re-checks `url.path` of the statement import routes: they hold ids only, no e-mail.
- Result: nothing leaks today. No code fix is planned. If the re-check finds a leak, the task is
  `blocked` with the finding.
- `docs/guides/logging.md` already holds the log privacy rule. The new docs repeat it for spans.

### D8. service.version
Decision: add `service.version=${JBH_SERVICE_VERSION:unknown}` to the resource attributes list
(see D1). Same env var name and default as `jbh-gateway`. Quarkus OTel sets `service.version`
from the artifact version at build time, which is `1.0.0-SNAPSHOT` and never changes. A
value in `quarkus.otel.resource.attributes` overrides that default (Quarkus docs: the property
adds or overrides resource attributes and can be overridden at run time). Rejected:
`${JBH_SERVICE_VERSION:${quarkus.application.version}}` (shows the same snapshot number for every
build, which looks like a real version but is not).

### D9. Existing test that guards the old values
`jbh-z-assembly/src/test/java/com/jbh/assembly/config/ObservabilityConfigPropertiesTest.java`
reads `application.properties` and fails after D1 and D3. Update it (same methods, renamed where
the old name would lie, no new test methods). Expected values:

| Test (today) | Today asserts | New expected value |
|---|---|---|
| `shouldReadEndpointFromEnvWhenConfigured` | `%prod.` endpoint starts with `${OTEL_EXPORTER_OTLP_ENDPOINT:` | `quarkus.otel.exporter.otlp.endpoint` equals `${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14317}` |
| `shouldLeaveBaseEndpointUnset` | base endpoint is null | `%prod.` and `%dev.` endpoint keys are null (one line only). Rename to `shouldHaveOneEndpointLineForAllProfiles` |
| `shouldExportByDefaultInDevToLocalStack` | `%dev.sdk.disabled` is `${OTEL_SDK_DISABLED:false}` and `%dev.` endpoint is `http://localhost:4317` | `%dev.quarkus.otel.sdk.disabled` is null; `%dev.jbh.otel.export-off` is `${OTEL_SDK_DISABLED:false}`; the `%dev.` endpoint assertion is removed |
| `shouldDisableOtelSdkByDefaultWhenNoEnvVar` | `quarkus.otel.sdk.disabled` is `${OTEL_SDK_DISABLED:true}` | `quarkus.otel.sdk.disabled` is `false`; `jbh.otel.export-off` is `${OTEL_SDK_DISABLED:true}`. Rename to `shouldKeepSdkOnAndExportOffByDefaultWhenNoEnvVar` |
| `shouldSampleAllByDefaultWhenNoSamplerEnvVar` | `quarkus.otel.traces.sampler.arg` is `${OTEL_TRACES_SAMPLER_ARG:1.0}` | same, plus `quarkus.otel.traces.sampler` is `${jbh.otel.sampler.${jbh.otel.export-off}:always_off}`; `jbh.otel.sampler.true` is `always_off`; `jbh.otel.sampler.false` is `${OTEL_TRACES_SAMPLER:parentbased_traceidratio}` |
| `shouldExportLogsToOtelOnlyInDev` | `%dev.` logs enabled `true`, base null | `quarkus.otel.logs.enabled` is `true`; `%dev.quarkus.otel.logs.enabled` is null; `jbh.otel.logs-handler.true` is `false`; `jbh.otel.logs-handler.false` is `true`; `quarkus.otel.logs.handler.enabled` is `${jbh.otel.logs-handler.${jbh.otel.export-off}:false}`. Rename to `shouldBuildOtelLogsInEveryProfileAndSwitchByExportFlag` |
| `shouldSetEnvPerProfileWhenTagging` | base `prod`, `%dev.` `dev`, `%test.` `test` | base `${JBH_ENV:prod}`, `%dev.` `dev`, `%test.` `test`; plus `quarkus.otel.resource.attributes` is `service.namespace=jbh,deployment.environment.name=${jbh.metrics.env},service.version=${JBH_SERVICE_VERSION:unknown}` |

All other tests stay unchanged and must pass: log levels, JSON only in prod, flat MDC fields,
datasource metrics `true`, dev console format has `traceId=%X{traceId}` and `spanId=%X{spanId}`,
no `quarkus.observability.enabled`. `MetricsTagsConfigTest` is untouched and must pass.
Unused imports (`assertNotNull`, `assertTrue`) stay only if still used: the dev console format test
uses both.

### D10. Horizontal scaling
Nothing here holds shared state. Properties are read-only config. The new filter and the request
filter keep no state between requests (MDC is per thread and cleared at the end of each request).
Two instances behave the same. See `Concurrency`.

## Test contract

- Unit tests: none new (the user excluded unit tests for the whole JBH-55 plan, even with
  Test-First on). No test blueprint group in tasks.md. Tests of removed code: none exist.
- Updated test: `ObservabilityConfigPropertiesTest` (D9 holds the expected values).
- Integration tests: none new. Existing suites must pass: `mvn clean verify` (PMD and SpotBugs
  included). Database tests (Docker, opt-in) do not read the changed properties.
- Manual check by the user, with the service running and one request sent:
  `curl -s localhost:7777/q/metrics | grep 'http_server_requests_seconds_bucket' | grep -o 'le="[^"]*"' | sort -u`
  must show 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0 and +Inf.
  A response must have no `X-Tracking-ID` and no `X-Request-ID` header
  (`curl -si .../q/health | grep -i 'x-.*-id'` is empty).
  With `OTEL_SDK_DISABLED=false` and Alloy running: one request with a `traceparent` header shows
  the same trace id in the log line, in Tempo, and in the gateway call to `jbh-iam`.

## Database

No database change. No table, column, index, constraint, view or migration.

## Concurrency

none. The new `MeterFilter` is a stateless singleton with an immutable constant. The filter and
`GatewayClientBuilder` changes keep no shared state. Property changes have no runtime state. No
check-then-act, no async code, no shared writer. With 2 or more instances nothing is shared, so
there is nothing to protect.

## Not included

- `../jbh-deploy` (system map, compose env vars): prod env vars are JBH-84. Not edited here.
- Turning prod export on (JBH-84).
- Any change in Alloy or in other services or repos.
- A Java `Sampler` bean (rejected, D3).
- Percentile histograms, or buckets for other meters than `http.server.requests`.
- Unit tests (excluded by the user).
- Changing the `jbh-gateway-client` version or the gateway client itself.

## Risks

- A typo in a `jbh.otel.*` key falls back to export off, not to an error. The properties test
  guards the exact keys and values.
- `${jbh.metrics.env}` and `${JBH_SERVICE_VERSION:unknown}` inside
  `quarkus.otel.resource.attributes` are config expressions. If they did not expand, the
  attribute would show raw text. Quarkus supports nested expressions. User check: start with
  `OTEL_SDK_DISABLED=false` and read the resource attributes in Tempo or Loki.
- Moving the dev endpoint from 4317 to 14317 changes the default for developers who run
  `quarkus:dev` against the shared LGTM stack. They can set
  `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317`. The docs say so.
- Removing `X-Tracking-ID` / `X-Request-ID` could break an unknown reader. The plan checked that
  the app and the front end never send or read them. Dashboards must use `traceId`.
- The preferences module may lack the OpenTelemetry API on its compile classpath (see D6).
