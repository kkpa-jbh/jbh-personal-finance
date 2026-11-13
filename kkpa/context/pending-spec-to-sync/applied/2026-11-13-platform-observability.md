# Sync proposal — platform-observability

> Staged by `kkpa-context-curate from-spec`. This is a **draft** of KB edits derived from one
> approved OpenSpec capability spec. Review/edit the blocks below, then run
> `/kkpa-context-curate apply-sync` to write them into the real KB. Nothing here touches the
> canonical KB until applied. This file is self-contained — it embeds the proposed content, so it
> stays valid even after the OpenSpec change folder is archived/moved.

Target guide: `architecture/observability.md`
Source spec:  `openspec/specs/platform-observability/spec.md`
Generated:    `2026-11-13`
Status: APPLIED 2026-11-13

---

## [guide] ## Glossary — REPLACE

- **Known as:** `observability`, `logging`, `logs`, `traces`, `tracing`, `metrics`, `OpenTelemetry`, `OTel`, `/q/metrics`, `telemetry`
- **Internal name:** `platform-observability`
- **Scope:** cross-cutting, all modules. Logs (level + format + content), traces (OTLP), metrics (Prometheus).

## [guide] ## How maintenance works — REPLACE

1. **Log level per profile.** `com.jbh` logs at INFO in `prod` and DEBUG in `dev`.
2. **Log format per profile.** `prod`: one JSON object per line. `traceId`, `spanId` and the MDC fields
   (`trackingId`, `userId`, `productId`, `transactionId`, `module`) are top-level fields.
   `dev` and `test`: plain text.
3. **Traces.** The service always creates traces, but exports them over OTLP only when env vars turn
   the SDK on (`OTEL_SDK_DISABLED=false`, `OTEL_EXPORTER_OTLP_ENDPOINT`). Sampling comes from
   `OTEL_TRACES_SAMPLER_ARG`; the default is 100%. With no env var: no export and no exporter error in the log.
   The trace id of a request equals the `traceId` in its log lines.
4. **Metrics.** JVM, HTTP server and datasource metrics at `/q/metrics` (Prometheus format). Every metric
   has the tags `service="jbh-personal-finance"` and `env`.
5. **AI provider spans.** Each outgoing call to Azure Document Intelligence, OpenRouter, Gemini and Jev is
   its own span with a duration, a child of the request trace that started the statement import.
   A failed call sets the span status to error and records the exception type, never the provider answer.
6. **Local dev.** The service starts without Docker and never starts a telemetry container itself. In dev it
   sends traces and logs to the shared local stack at `localhost:4317` by default
   (`make lgtm up` in kkpa-docker-lab; Grafana on port 3000).
7. **No side effects.** The REST API, the Consul registration (name, tags, health path), the health
   endpoints and the existing tests do not change because of observability.

## [guide] ## Conventions & gotchas — APPEND

- **Never log at DEBUG in prod.** A new `com.jbh` log category must not set a level without a profile
  prefix, or it overrides the `%prod` INFO level.
  _Source: spec platform-observability — Requirement: Log level per profile._
- **Put request context in MDC, not in the message.** The JSON log lifts MDC fields to top-level fields,
  so a new context field belongs in MDC to be queryable.
  _Source: spec platform-observability — Requirement: JSON console logs in prod._
- **Export is opt-in by env var.** Do not hard-code an OTLP endpoint or turn the SDK on in config; prod
  must run with no collector and no exporter error.
  _Source: spec platform-observability — Requirement: Traces export only when asked._
- **No high-cardinality metric labels.** Never use a user id, a product id or a raw path as a label value.
  The `uri` label must be the path template (for example `/jbh-api/products/{productId}/movements`).
  _Source: spec platform-observability — Requirement: Metrics for Prometheus._
- **Every metric carries `service` and `env`.** A new custom metric must keep both common tags.
  _Source: spec platform-observability — Requirement: Metrics for Prometheus._
- **A new external AI provider needs its own span.** Record the duration; on failure set status error
  with the exception type only — never the prompt or the answer in the span.
  _Source: spec platform-observability — Requirement: AI provider calls are spans._
- **INFO, WARN and ERROR lines carry ids, periods, counts and enum names only.** Never: amounts, balances,
  statement or OCR text, LLM prompts or answers, IBAN or account numbers, e-mail addresses, client IP
  addresses, API keys, transaction lists, product names, movement descriptions, or a whole domain object.
  Logs leave the VPS (Grafana Cloud).
  _Source: spec platform-observability — Requirement: No personal data in INFO+ logs._
- **E-mail errors: do not log the exception message.** It can hold the recipient address; log the
  exception type instead. Same for the subject.
  _Source: spec platform-observability — Requirement: No personal data in INFO+ logs._
- **Dev must start without Docker.** Do not add a Dev Service that starts a telemetry container.
  _Source: spec platform-observability — Requirement: Local telemetry stack is shared and external._

## [index] ## Architecture topics — ADD ROWS

| observability | `architecture/observability.md` |
| logging | `architecture/observability.md` |
| log privacy (no personal data in logs) | `architecture/observability.md` |
| traces / OpenTelemetry | `architecture/observability.md` |
| metrics / `/q/metrics` | `architecture/observability.md` |
