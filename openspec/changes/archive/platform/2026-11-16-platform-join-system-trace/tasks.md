# Tasks: platform-join-system-trace

Test-First is on, but the user excluded unit tests for the whole JBH-55 plan. So there is no test
blueprint group. The one existing test that breaks is updated in group 1.
Tick each box live. Do not edit a task title or its acceptance text.

## 1. Properties and the properties test (jbh-z-assembly)
Depends on: none.

- [x] 1.1 In `jbh-z-assembly/src/main/resources/application.properties`, block OBSERVABILITY, apply
  design D1, D2, D3 and D8: `quarkus.otel.logs.enabled=true` (no profile prefix); one endpoint line
  `quarkus.otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14317}`; the
  export switch lines; `jbh.metrics.env=${JBH_ENV:prod}` (keep `%dev` and `%test`); resource
  attributes with `service.version=${JBH_SERVICE_VERSION:unknown}`; remove the lines listed as
  removed in D1; update the comments (MDC list without `trackingId`; dev console comment; one
  plain-English comment block above the switch). Acceptance: the file holds exactly the keys and
  values of D1 and D3, and has no `%dev.`/`%prod.` endpoint key and no `OTEL_SDK_DISABLED` outside
  the two `jbh.otel.export-off` lines.
- [x] 1.2 Update `jbh-z-assembly/src/test/java/com/jbh/assembly/config/ObservabilityConfigPropertiesTest.java`
  as the D9 table says. Same test methods (rename only the five named there). No new test
  method. No other test changes. Acceptance: every assertion matches the values in D9.

## 2. Latency histogram (jbh-z-assembly)
Depends on: none.

- [x] 2.1 In `jbh-z-assembly/src/main/java/com/jbh/assembly/config/MetricsTagsConfig.java`, add the
  second `MeterFilter` `httpServerLatency()` as D4 says, and update the class Javadoc. Do not
  change the constructor or `commonTags()`. PMD rules from `code-quality.md` apply (final
  variables, no literal in `if`, `LongVariable`, `AvoidDuplicateLiterals`). Acceptance:
  `http.server.requests` gets the 11 buckets of D4; other meters keep their config;
  `MetricsTagsConfigTest` still passes.

## 3. Remove trackingId and requestId (jbh-finance)
Depends on: none. Files are different from groups 1, 2 and 4.

- [x] 3.1 In `jbh-finance/jbh-finance-infra/.../common/logging/LoggingFilter.java`, apply D5: remove
  the tracking id and request id constants, extract methods, properties, response headers and the
  unused import. Keep `userId`, `module`, the two log lines and `LoggingContext.clear()`.
  Acceptance: no `trackingId`, `requestId`, `X-Tracking-ID` or `X-Request-ID` in the file.
- [x] 3.2 In `jbh-finance/jbh-finance-application/.../common/logging/LoggingContext.java`, apply D5.
  Depends on: 3.1 (so no caller is left). Re-run a CodeGraph or grep check for other callers
  before the removal. Acceptance: no `trackingId`, `TRACKING_ID`, `requestId`, `REQUEST_ID` in main
  code of the repo.

## 4. Join the trace in the gateway client (jbh-finance, jbh-preferences)
Depends on: none.

- [x] 4.1 `jbh-finance/jbh-finance-infra/.../gateway/GatewayClientBuilder.java`: inject
  `io.opentelemetry.api.OpenTelemetry` and call `.openTelemetry(...)` as D6 says. Acceptance: the
  built client gets the Quarkus instance; the constructor has at most 3 parameters.
- [x] 4.2 `jbh-preferences/jbh-preferences-infra/.../gateway/GatewayClientBuilder.java`: the same.
  Check that the OpenTelemetry API is on the compile classpath of the module. If not, report
  `blocked` and name the missing dependency (D6). Acceptance: same as 4.1.

## 5. Safety check of span attributes (read-only)
Depends on: none.

- [x] 5.1 Re-check the four `@WithSpan` adapters (D7) and the `url.path` of the import routes. Edit
  nothing if clean. Acceptance: the report states "no personal data" with the files read; or the
  task is `blocked` with the finding.

## 6. Docs
Depends on: 1, 3 (so the text matches the final names).

- [x] 6.1 In `.claude/infrastructure/deployment.md`, rewrite the "Observability (JBH-60)" section as
  "Observability" (also fix the line that says logs have `trackingId`, and the "request IDs" bullet
  in Centralized Logging): signals (trace id in logs, OTLP logs, Prometheus metrics with the
  latency buckets), env vars (`OTEL_SDK_DISABLED` = export only, `OTEL_EXPORTER_OTLP_ENDPOINT`,
  `OTEL_TRACES_SAMPLER_ARG`, `JBH_ENV`, `JBH_SERVICE_VERSION`), the default (export off, dev on),
  the known limit (sampled=0 with export off), the dev endpoint change to `14317`, and the rule: no
  personal data in logs or span attributes. Acceptance: no `trackingId` in the file; every env var
  of D1 and D3 is named.
- [x] 6.2 In `docs/guides/logging.md`, remove the `trackingId` / `requestId` lines, examples and the
  advice "use `trackingId` to find all lines of a request" (use `traceId` instead). Acceptance: no
  `trackingId`, `requestId`, `X-Tracking-ID` in the file.

## 7. Leader checks (not for workers)
Depends on: 1 to 6.

- [ ] 7.1 The leader runs `mvn clean verify` (tests, PMD, SpotBugs) and the manual checks in
  design.md `Test contract`. Workers run nothing.

Implementation size (9 files: `application.properties`, `ObservabilityConfigPropertiesTest`,
`MetricsTagsConfig`, `LoggingFilter`, `LoggingContext`, two `GatewayClientBuilder`, `deployment.md`,
`logging.md`; the OpenSpec artifacts add 4 files, ~420 lines).

Size estimate: 9 files, ~300 changed lines (tests and docs included)
