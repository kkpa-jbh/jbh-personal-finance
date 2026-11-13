# platform-observability Specification

## Purpose
TBD - created by archiving change platform-add-observability. Update Purpose after archive.
## Requirements
### Requirement: Log level per profile
The service SHALL log the `com.jbh` packages at INFO in the `prod` profile and at DEBUG in the `dev` profile.

#### Scenario: Prod does not log DEBUG
- GIVEN the service runs with the `prod` profile
- WHEN code in `com.jbh` writes a DEBUG line and an INFO line
- THEN only the INFO line reaches the console

#### Scenario: Dev keeps DEBUG
- GIVEN the service runs with the `dev` profile
- WHEN code in `com.jbh` writes a DEBUG line
- THEN the line reaches the console

### Requirement: JSON console logs in prod
The service SHALL write the console log as one JSON object per line in the `prod` profile, and as plain text in the `dev` and `test` profiles.

#### Scenario: Prod line is JSON with context fields
- GIVEN the `prod` profile and a request with trace context
- WHEN the service handles the request and writes an INFO line
- THEN the line is valid JSON
- AND it has the top-level fields `traceId` and `spanId`
- AND it has the MDC fields that are set for the request (`trackingId`, `userId`, `productId`, `transactionId`, `module`) as top-level fields

#### Scenario: Dev and test stay readable
- GIVEN the `dev` or the `test` profile
- WHEN the service writes a log line
- THEN the line is plain text, not JSON

### Requirement: Traces export only when asked
The service SHALL create traces, and export them over OTLP only when an env var turns the OpenTelemetry SDK on. The endpoint and the sampling come from env vars. The default sampling is 100%.

#### Scenario: No env var, no export
- GIVEN no OpenTelemetry env var is set
- WHEN the service runs and handles requests
- THEN no span is exported
- AND the log holds no OTLP exporter error

#### Scenario: Env vars turn export on
- GIVEN `OTEL_SDK_DISABLED=false` and `OTEL_EXPORTER_OTLP_ENDPOINT=<collector url>`
- WHEN the service handles a request
- THEN a trace for the request is sent to that endpoint
- AND the trace id equals the `traceId` in the log lines of the request

#### Scenario: Sampling is changed by env var
- GIVEN `OTEL_TRACES_SAMPLER_ARG=0.1`
- WHEN the service runs
- THEN about 10% of the root traces are sampled

### Requirement: Metrics for Prometheus
The service SHALL publish JVM, HTTP server and datasource metrics at `/q/metrics` in Prometheus format. Every metric SHALL carry the tags `service` and `env`. No metric SHALL carry a user id, a product id or a raw path as a label value.

#### Scenario: Common tags
- GIVEN the service runs with the `prod` profile
- WHEN `GET /q/metrics` is called
- THEN the JVM, HTTP and datasource metrics are present
- AND each has `service="jbh-personal-finance"` and `env="prod"`

#### Scenario: No high-cardinality label
- GIVEN requests to `/jbh-api/products/{productId}/movements` for 3 different product ids
- WHEN `GET /q/metrics` is called
- THEN the `uri` label holds the path template, not the 3 raw ids
- AND no label named `userId` or `productId` exists

### Requirement: AI provider calls are spans
The service SHALL record each outgoing call to Azure Document Intelligence, OpenRouter, Gemini and Jev as a span with its duration. A failed call SHALL set the span status to error.

#### Scenario: Statement import shows provider spans
- GIVEN tracing is on and a statement import runs
- WHEN the trace is read in the trace backend
- THEN it holds one span for the Azure call, one for the LLM call and one for the Jev call
- AND each is a child span of the request trace of the import start
- AND each has a duration

#### Scenario: Provider failure
- GIVEN a provider call that throws an error
- WHEN its span is read
- THEN the span status is error
- AND the span holds the exception type, not the provider answer

### Requirement: No personal data in INFO+ logs
No INFO, WARN or ERROR log line SHALL write amounts, balances, statement or OCR text, LLM prompts or answers, IBAN or account numbers, e-mail addresses, client IP addresses, API keys, transaction lists or a whole domain object.

#### Scenario: Product and movement flow
- GIVEN a movement is added to a product with a balance of 1000
- WHEN the INFO lines of the flow are read
- THEN none holds the amount, the balance, the product name or the movement description
- AND they hold ids, periods, counts or enum names only

#### Scenario: Incoming request
- GIVEN a request arrives from a client with an IP address
- WHEN the INFO lines of the request are read
- THEN none holds the client IP address

#### Scenario: E-mail notification
- GIVEN a notification is sent to a user e-mail address
- WHEN the INFO and ERROR lines are read
- THEN none holds the address, the subject or an exception message that can hold the address

### Requirement: Local telemetry stack is shared and external
The service SHALL start in dev mode without Docker. The service SHALL NOT start a telemetry container itself.
In dev it SHALL send traces and logs to the shared local stack at `localhost:4317` by default.

#### Scenario: Default dev start without the stack
- GIVEN no observability env var and the local stack is not running
- WHEN the service starts with `quarkus:dev`
- THEN it starts as before
- AND no telemetry container is started

#### Scenario: Shared stack running
- GIVEN the local stack runs (`make lgtm up` in kkpa-docker-lab)
- WHEN a statement import runs in dev
- THEN its traces, logs and metrics can be seen in the local Grafana on port 3000

### Requirement: Existing behavior is unchanged
The change SHALL NOT change the REST API, the Consul registration, the health endpoints or the existing tests.

#### Scenario: Health and registration
- GIVEN the service starts
- WHEN `GET /q/health` is called
- THEN it returns status `UP`
- AND the service is registered in Consul with the same name, tags and health check path as before

