## MODIFIED Requirements

### Requirement: JSON console logs in prod
The service SHALL write the console log as one JSON object per line in the `prod` profile, and as plain text in the `dev` and `test` profiles. The log SHALL carry the trace id of the request as `traceId`. It SHALL NOT carry a `trackingId` or a `requestId`.

#### Scenario: Prod line is JSON with context fields
- GIVEN the `prod` profile and a request with trace context
- WHEN the service handles the request and writes an INFO line
- THEN the line is valid JSON
- AND it has the top-level fields `traceId` and `spanId`
- AND it has the MDC fields that are set for the request (`userId`, `productId`, `transactionId`, `module`) as top-level fields
- AND it has no field `trackingId` and no field `requestId`

#### Scenario: Dev and test stay readable
- GIVEN the `dev` or the `test` profile
- WHEN the service writes a log line
- THEN the line is plain text, not JSON

### Requirement: Traces export only when asked
The service SHALL create traces in every profile and in every mode. It SHALL send traces and logs over OTLP only when the export is on. The env var `OTEL_SDK_DISABLED` SHALL switch only the export, never the tracing. The default is export off in the `prod` and `test` profiles, and export on in the `dev` profile. The endpoint and the sampling come from env vars. The default sampling is 100% when the export is on.

#### Scenario: No env var in prod, no export, trace id still in logs
- GIVEN the `prod` profile and no OpenTelemetry env var
- WHEN the service handles a request that has a `traceparent` header
- THEN no span and no log record is sent over OTLP
- AND the log holds no OTLP exporter error
- AND the log lines of the request carry the `traceId` of the incoming `traceparent`

#### Scenario: Env var turns export on
- GIVEN `OTEL_SDK_DISABLED=false` and `OTEL_EXPORTER_OTLP_ENDPOINT=<collector url>`
- WHEN the service handles a request
- THEN a trace for the request and its log records are sent to that endpoint
- AND the trace id equals the `traceId` in the log lines of the request

#### Scenario: Invalid switch value is safe
- GIVEN `OTEL_SDK_DISABLED=1`
- WHEN the service starts
- THEN it starts
- AND the export is off

#### Scenario: Sampling is changed by env var
- GIVEN the export is on and `OTEL_TRACES_SAMPLER_ARG=0.1`
- WHEN the service runs
- THEN about 10% of the root traces are sampled

### Requirement: Local telemetry stack is shared and external
The service SHALL start in dev mode without Docker. The service SHALL NOT start a telemetry container itself.
In dev it SHALL send traces and logs to the collector at `localhost:14317` by default. One setting, `OTEL_EXPORTER_OTLP_ENDPOINT`, SHALL set the endpoint for every profile.

#### Scenario: Default dev start without the collector
- GIVEN no observability env var and the collector is not running
- WHEN the service starts with `quarkus:dev`
- THEN it starts as before
- AND no telemetry container is started

#### Scenario: Collector running
- GIVEN the collector runs on `localhost:14317`
- WHEN a statement import runs in dev
- THEN its traces and logs can be seen in the local Grafana

#### Scenario: Endpoint from env var
- GIVEN `OTEL_EXPORTER_OTLP_ENDPOINT=http://jbh-alloy:4317` in any profile
- WHEN the export is on
- THEN the service sends to `http://jbh-alloy:4317`

## ADDED Requirements

### Requirement: One trace across services
The service SHALL continue the trace of the caller. It SHALL pass the same trace to every call it makes through `jbh-gateway-client`, so the trace id is the same on every hop.

#### Scenario: Incoming trace is joined
- GIVEN a request with a valid `traceparent` header
- WHEN the service handles it
- THEN its server span and its log lines use the trace id of the header

#### Scenario: Outgoing gateway call continues the trace
- GIVEN a request that makes a call through the gateway client (for example to find the user id)
- WHEN the call goes out
- THEN it carries a `traceparent` header with the same trace id as the request
- AND the trace holds one client span for the call

#### Scenario: Export off keeps the trace id but not the sampling
- GIVEN the export is off
- WHEN the service makes a gateway call
- THEN the `traceparent` header holds the same trace id
- AND its sampled flag is `00`

### Requirement: No request id headers
The service SHALL NOT read or write the headers `X-Tracking-ID` and `X-Request-ID`. The trace id is the only request identifier.

#### Scenario: Response has no own id header
- GIVEN any request
- WHEN the service answers
- THEN the response has no `X-Tracking-ID` and no `X-Request-ID` header

### Requirement: Resource attributes
The service SHALL describe itself in every exported trace and log with the resource attributes `service.name`, `service.namespace=jbh`, `deployment.environment.name` and `service.version`. The value of `deployment.environment.name` SHALL equal the metric tag `env`. The value of `service.version` SHALL come from `JBH_SERVICE_VERSION`, and SHALL be `unknown` when it is not set.

#### Scenario: Prod attributes
- GIVEN the `prod` profile, `JBH_ENV=prod` and `JBH_SERVICE_VERSION=abc123`
- WHEN a span or a log record is exported
- THEN its resource has `service.namespace=jbh`, `deployment.environment.name=prod` and `service.version=abc123`

#### Scenario: No version env var
- GIVEN `JBH_SERVICE_VERSION` is not set
- WHEN a span is exported
- THEN its resource has `service.version=unknown`

#### Scenario: Metric tag and resource agree
- GIVEN `JBH_ENV=staging` in the `prod` profile
- WHEN `GET /q/metrics` is called and a span is exported
- THEN the metrics have `env="staging"`
- AND the resource has `deployment.environment.name=staging`

### Requirement: HTTP latency histogram
The service SHALL publish the latency of `http.server.requests` as a histogram with these upper bounds: 5 ms, 10 ms, 25 ms, 50 ms, 100 ms, 250 ms, 500 ms, 1 s, 2.5 s, 5 s and 10 s. Other meters SHALL keep their own bucket settings.

#### Scenario: Buckets on the HTTP server timer
- GIVEN the service handled at least one request
- WHEN `GET /q/metrics` is called
- THEN `http_server_requests_seconds_bucket` has the `le` values 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0 and `+Inf`

#### Scenario: Other meters are unchanged
- GIVEN the service handled at least one request
- WHEN `GET /q/metrics` is called
- THEN every meter other than `http.server.requests` has the same bucket series as before this change

### Requirement: No personal data in span attributes
The spans of the AI provider calls SHALL NOT carry statement content, amounts, descriptions or e-mail addresses as attributes.

#### Scenario: Provider span attributes
- GIVEN a statement import runs and the spans of Azure, the LLM and Jev are read
- WHEN their attributes are listed
- THEN none holds statement text, an amount, a movement description or an e-mail address
