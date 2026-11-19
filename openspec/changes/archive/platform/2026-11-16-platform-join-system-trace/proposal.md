# Proposal: platform-join-system-trace

Source ticket: JBH-81 - https://linear.app/kkpa/issue/JBH-81/jbh-personal-finance-otlp-logs-one-trace-id-latency-histograms

## Why

JBH-55 wants one trace id for a request across the whole JBH system, in logs, traces and metrics.
Today this service does not join it:

- It makes its own random `trackingId` and `requestId` for each request. They are log-only and
  nobody reads them. They hide the real `traceId`.
- Calls to `jbh-iam` through the gateway do not continue the trace, because the gateway client
  gets no `OpenTelemetry` instance.
- OTLP logs go out only in `%dev`. The endpoint is fixed in two profile lines.
- The `OTEL_SDK_DISABLED` switch turns ALL OpenTelemetry off. Then there is no `traceId` in the
  logs and no `traceparent` handling.
- `http.server.requests` has no latency buckets, so Grafana cannot draw latency percentiles.
- Metric `env` and resource attributes are not read from `JBH_ENV`.

## What changes

- **Export switch (reused from `jbh-notification`):** `quarkus.otel.sdk.disabled=false` always.
  `OTEL_SDK_DISABLED` now stops only the OTLP export, through the `jbh.otel.*` lookup keys.
  Tracing stays on in every profile. Default is export OFF, also in prod.
- **OTLP logs** are built in every profile; the run-time switch decides if they are sent.
- **One endpoint line** for all profiles: `${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14317}`.
- **`jbh.metrics.env`** reads `${JBH_ENV:prod}`. `%dev` and `%test` overrides stay.
- **Resource attributes:** `service.namespace=jbh`, `deployment.environment.name` (same value as
  the metric tag `env`), `service.version` from `JBH_SERVICE_VERSION` (default `unknown`).
- **Latency histogram:** a second `MeterFilter` in `MetricsTagsConfig` sets 11 shared buckets for
  `http.server.requests` only.
- **Remove `trackingId` / `X-Tracking-ID` and `requestId` / `X-Request-ID`** from `LoggingFilter`
  and `LoggingContext`. `traceId` and `spanId` stay in the MDC (OpenTelemetry fills them).
- **Join the trace across services:** both `GatewayClientBuilder` classes pass the Quarkus
  `OpenTelemetry` to `JbhGatewayClientBuilder.openTelemetry(...)`.
- **Safety check** of `@WithSpan` adapters: no personal data in span attributes.
- **Docs:** "Observability" section in `.claude/infrastructure/deployment.md` and the
  `trackingId` lines in `docs/guides/logging.md`.

## Not in this change

Turning prod export on (JBH-84), any edit in `../jbh-deploy`, Alloy, or other services, and unit
tests (excluded by the user for the whole JBH-55 plan).

## Impact

- **Breaking:** no API break. The response headers `X-Tracking-ID` and `X-Request-ID` are no
  longer sent, and the log fields `trackingId` and `requestId` disappear. A search of the repo
  and the plan found no reader of them. Dashboards must use `traceId`.
- **Modules affected (one change, one domain `platform`):** `jbh-z-assembly`,
  `jbh-finance/jbh-finance-infra`, `jbh-finance/jbh-finance-application` (logging context only),
  `jbh-preferences/jbh-preferences-infra`, and `.claude/infrastructure/deployment.md`. All edits
  are small and cross-cutting, so this is one change and not a roadmap.
- **Database:** none. **Hot read paths:** none. The histogram adds 12 series (11 buckets and
  `+Inf`) per tag set of `http.server.requests`.
- **Dependency:** `jbh-gateway-client` 1.0.0-SNAPSHOT (already in `~/.m2`, has `openTelemetry(...)`).
  No version edit, no new Maven dependency.
- **Capability:** delta against `platform-observability`.
