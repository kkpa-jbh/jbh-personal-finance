# Proposal: platform-add-observability

Source ticket: JBH-60 - https://linear.app/kkpa/issue/JBH-60/jbh-personal-finance-json-logs-opentelemetry-traces-and-metrics-safe
Owner: platform (cross-cutting). Archive domain: `platform`.
Breaking: no. The REST API, the Consul registration and the health endpoints do not change.
The prod console log changes from text to JSON. Nothing in the repo reads the text format.
Modules affected: `jbh-z-assembly` (deps, config, one new class), `jbh-finance` (infra: AI adapters,
dispatcher, log lines; application and domain: log lines only), `jbh-notification` (infra: log lines
only). `jbh-preferences` and `jbh-products`: none.
Parent: part 5 of JBH-55 (observability with Grafana Cloud). Alloy (`jbh-alloy`) comes in JBH-56.

## Why

- Prod logs DEBUG. `quarkus.log.category."com.jbh.**".level=DEBUG` has no profile prefix.
- Logs leave the VPS (Docker logs, then Alloy, then Grafana Cloud). Plain text is hard to query. The
  MDC fields (`trackingId`, `userId`, `productId`, `transactionId`, `module`) need to be real fields.
- There are no traces and no metrics. A slow statement import cannot be split into Azure, LLM and Jev time.
- Some INFO+ log lines write balances, amounts or whole objects. Once logs leave the VPS this is a
  privacy problem.

## What changes

1. Log level: `%dev` keeps DEBUG, `%prod` uses INFO (the base line has no level for `com.jbh`).
2. JSON console logs for `%prod` only (`quarkus-logging-json`). MDC fields are written as top-level fields.
3. Traces with `quarkus-opentelemetry`. The endpoint and the sampler come from env vars. The SDK is
   off until an env var turns it on, so prod has no exporter errors before Alloy exists.
4. Metrics with Micrometer and the Prometheus registry (`/q/metrics`). Common tags `service` and `env`.
   Datasource metrics on.
5. Child spans for Azure Document Intelligence, OpenRouter, Gemini and Jev (`@WithSpan` on the
   adapters). The OTel context is passed to the background import thread.
6. Log audit: INFO/WARN/ERROR lines in all modules lose amounts, balances, whole objects, e-mail
   addresses and free-text product names.
7. Local dev: `quarkus-observability-devservices-lgtm` (provided scope), off by default.
8. Delete the unused `logback-spring.xml` and fix `docs/guides/logging.md`.

## Not included

- Ticket point 6 (the trace-aware `jbh-gateway-client` for `findUserId`). Deferred by the user. Later
  it is only a client version bump.
- Ticket point 7 (env vars in `jbh-deploy/compose.yaml`). It lands in `jbh-deploy` with Alloy (JBH-56).
- The Grafana Cloud checks of the ticket. They move to JBH-56 and JBH-61.
- OTel metrics and OTel logs (`quarkus.otel.metrics.enabled`, `quarkus.otel.logs.enabled`). Both
  are preview in Quarkus 3.33. Micrometer is stable.
- JDBC spans, DEBUG log lines, the `llmcompare/` adapters (dev-only comparison, not a prod path).

## Database and read paths

No database change. No hot read path is touched. `/q/metrics` is a new read path: one scrape every
few seconds from Alloy on `jbh-private`. The gateway routes only `/jbh-api/**`, so it is not public.

## Goals (acceptance, user-confirmed)

- G1: `%prod` logs `com.jbh` at INFO. `%dev` keeps DEBUG.
- G2: `%prod` console logs are JSON with `traceId`, `spanId` and the MDC fields. Dev and test stay plain text.
- G3: With no OTLP env var, export is off and no exporter error appears.
- G4: `/q/metrics` shows JVM, HTTP and datasource metrics with tags `service` and `env`, and no user id or product id label.
- G5: With local LGTM on, a statement import shows the AI calls as child spans.
- G6: No INFO+ log line writes amounts, statement text, IBAN, e-mail or keys.
- Must not change: REST API, Consul registration (`ConsulServiceRegistration`), health endpoints, existing tests.

## Why one change and not a roadmap

The config work is in one module (`jbh-z-assembly`). The log audit touches 3 modules, but each edit is
one log argument and has no logic or contract. A roadmap with one tier per module would cost more than it saves.
