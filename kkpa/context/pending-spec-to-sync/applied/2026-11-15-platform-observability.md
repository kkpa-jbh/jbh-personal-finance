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

## [guide] ## Conventions & gotchas — REPLACE


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
- **E-mail notification logs are not this service's rules anymore.** The notification flow moved to
  the `jbh-notification` service, which owns its own log rules. Do not add e-mail sending code here.
  _Source: spec platform-observability — Requirement: No personal data in INFO+ logs._
- **Dev must start without Docker.** Do not add a Dev Service that starts a telemetry container.
  _Source: spec platform-observability — Requirement: Local telemetry stack is shared and external._
