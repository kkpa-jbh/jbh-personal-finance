# Sync proposal — platform-observability

> Staged by `kkpa-context-curate from-spec`. This is a **draft** of KB edits derived from one
> approved OpenSpec capability spec. Review/edit the blocks below, then run
> `/kkpa-context-curate apply-sync` to write them into the real KB. Nothing here touches the
> canonical KB until applied. This file is self-contained.

Target guide: `architecture/observability.md`
Source spec:  `openspec/specs/platform-observability/spec.md`
Generated:    `2026-11-15`
Status: APPLIED 2026-11-15

---

Why this proposal: change `platform-join-system-trace` (JBH-81) changed the spec. Tracing is now on
in every mode, `OTEL_SDK_DISABLED` stops only the export, `trackingId` / `requestId` are gone, and the
spec adds one trace across services, resource attributes, the HTTP latency histogram and the span
privacy rule. Two sections are REPLACE, because their old text is now wrong (MDC list with
`trackingId`, dev endpoint `4317`, "do not turn the SDK on in config").

## [guide] ## Glossary — REPLACE

- **Known as:** `observability`, `logging`, `logs`, `traces`, `tracing`, `trace id`, `traceparent`, `metrics`, `latency histogram`, `OpenTelemetry`, `OTel`, `/q/metrics`, `telemetry`
- **Internal name:** `platform-observability`
- **Scope:** cross-cutting, all modules. Logs (level + format + content), traces (OTLP), metrics (Prometheus).

## [guide] ## How maintenance works — REPLACE

1. **Log level per profile.** `com.jbh` logs at INFO in `prod` and DEBUG in `dev`.
2. **Log format per profile.** `prod`: one JSON object per line. `traceId`, `spanId` and the MDC fields
   (`userId`, `productId`, `transactionId`, `module`) are top-level fields. There is no `trackingId` and
   no `requestId`. `dev` and `test`: plain text.
3. **Traces are always on; only the export is switched.** The service creates traces in every profile.
   `OTEL_SDK_DISABLED` switches only the OTLP export of traces and logs, never the tracing. Default: export
   off in `prod` and `test`, on in `dev`. A bad value (for example `1`) keeps export off and the service
   starts. With export off, logs still carry the `traceId` of the incoming `traceparent`, and no exporter
   error is logged. Sampling comes from `OTEL_TRACES_SAMPLER_ARG`; the default is 100% when export is on.
4. **One trace across services.** The service joins the caller's `traceparent` and passes the same trace to
   every call through `jbh-gateway-client` (one client span per call). With export off, the outgoing
   `traceparent` keeps the trace id but has sampled flag `00`.
5. **No own request id.** The service does not read or write `X-Tracking-ID` or `X-Request-ID`. The trace id
   is the only request identifier.
6. **Resource attributes.** Every exported span and log record has `service.name`, `service.namespace=jbh`,
   `deployment.environment.name` (equals the metric tag `env`, from `JBH_ENV`) and `service.version`
   (from `JBH_SERVICE_VERSION`, `unknown` when not set).
7. **Metrics.** JVM, HTTP server and datasource metrics at `/q/metrics` (Prometheus format). Every metric
   has the tags `service="jbh-personal-finance"` and `env`. `http.server.requests` has fixed latency buckets:
   5 ms, 10 ms, 25 ms, 50 ms, 100 ms, 250 ms, 500 ms, 1 s, 2.5 s, 5 s, 10 s. Other meters keep their own buckets.
8. **AI provider spans.** Each outgoing call to Azure Document Intelligence, OpenRouter, Gemini and Jev is
   its own span with a duration, a child of the request trace that started the statement import.
   A failed call sets the span status to error and records the exception type, never the provider answer.
9. **Local dev.** The service starts without Docker and never starts a telemetry container itself. In dev it
   sends traces and logs to the collector at `localhost:14317` by default (Alloy's dev port).
   `OTEL_EXPORTER_OTLP_ENDPOINT` sets the endpoint for every profile (Docker: `http://jbh-alloy:4317`).
10. **No side effects.** The REST API, the Consul registration (name, tags, health path), the health
   endpoints and the existing tests do not change because of observability.

## [guide] ## Conventions & gotchas — REPLACE

- **Never log at DEBUG in prod.** A new `com.jbh` log category must not set a level without a profile
  prefix, or it overrides the `%prod` INFO level.
  _Source: spec platform-observability — Requirement: Log level per profile._
- **Put request context in MDC, not in the message.** The JSON log lifts MDC fields to top-level fields,
  so a new context field belongs in MDC to be queryable. Never add a second request id (`trackingId`,
  `requestId`): use `traceId`.
  _Source: spec platform-observability — Requirement: JSON console logs in prod._
- **`OTEL_SDK_DISABLED` switches only the export.** Never use `quarkus.otel.sdk.disabled` as the switch: it
  turns all OpenTelemetry off, so logs lose `traceId` and `traceparent` is not passed on. Prod must run
  with no collector and no exporter error. Do not hard-code an OTLP endpoint per profile.
  _Source: spec platform-observability — Requirement: Traces export only when asked._
- **Pass the trace to every outgoing gateway call.** A new gateway client builder must get the Quarkus
  `OpenTelemetry`, or the trace stops at this service.
  _Source: spec platform-observability — Requirement: One trace across services._
- **Export off changes the sampled flag downstream.** With export off, the outgoing `traceparent` has
  sampled `00`, so a parent-based service called next drops its spans. Switch export on in all services
  together.
  _Source: spec platform-observability — Requirement: One trace across services._
- **No `X-Tracking-ID` / `X-Request-ID`.** Do not add them back; the trace id is the request id.
  _Source: spec platform-observability — Requirement: No request id headers._
- **`deployment.environment.name` and the metric tag `env` come from one value.** Change `JBH_ENV` handling
  in one place, so traces and metrics never disagree.
  _Source: spec platform-observability — Requirement: Resource attributes._
- **No high-cardinality metric labels.** Never use a user id, a product id or a raw path as a label value.
  The `uri` label must be the path template (for example `/jbh-api/products/{productId}/movements`).
  _Source: spec platform-observability — Requirement: Metrics for Prometheus._
- **Every metric carries `service` and `env`.** A new custom metric must keep both common tags.
  _Source: spec platform-observability — Requirement: Metrics for Prometheus._
- **Latency buckets are shared across JBH services.** Do not change the `http.server.requests` bucket list
  in one service only, or p95 cannot be compared across services. Do not add buckets to other meters.
  _Source: spec platform-observability — Requirement: HTTP latency histogram._
- **A new external AI provider needs its own span.** Record the duration; on failure set status error
  with the exception type only — never the prompt or the answer in the span.
  _Source: spec platform-observability — Requirement: AI provider calls are spans._
- **No personal data in span attributes.** AI provider spans must not carry statement text, amounts,
  movement descriptions or e-mail addresses.
  _Source: spec platform-observability — Requirement: No personal data in span attributes._
- **INFO, WARN and ERROR lines carry ids, periods, counts and enum names only.** Never: amounts, balances,
  statement or OCR text, LLM prompts or answers, IBAN or account numbers, e-mail addresses, client IP
  addresses, API keys, transaction lists, product names, movement descriptions, or a whole domain object.
  Logs leave the VPS (Grafana Cloud).
  _Source: spec platform-observability — Requirement: No personal data in INFO+ logs._
- **E-mail notification logs are not this service's rules anymore.** The notification flow moved to
  the `jbh-notification` service, which owns its own log rules. Do not add e-mail sending code here.
  _Source: spec platform-observability — Requirement: No personal data in INFO+ logs._
- **Dev must start without Docker.** Do not add a Dev Service that starts a telemetry container.
  _Source: spec platform-observability — Requirement: Local telemetry stack is shared and external._

## [index] ## Architecture topics — ADD ROWS

| trace id / `traceparent` | `architecture/observability.md` |
| latency histogram / buckets | `architecture/observability.md` |
