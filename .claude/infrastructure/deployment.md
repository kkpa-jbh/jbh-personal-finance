# Deployment Strategy

## Single Deployable Unit

### Architecture Model

**Target:** Single deployable unit with well-defined modules

- All modules are packaged together into one deployment artifact
- Modules are separated logically but deployed as a monolith
- Clear module boundaries allow future microservices extraction if needed
- Runs on port 7777 (`quarkus.http.port`) and registers in Consul as `jbh-personal-finance`
- Runs as more than one instance. Every design must stay correct with 2 or more instances
  (see "Horizontal scaling" in `../CLAUDE.md`)

### Budget Constraint

**Cost-Effective Hosting:**

- No cloud provider dependencies (AWS, GCP, Azure)
- Design for self-hosted infrastructure
- Optimize for minimal resource footprint
- Use open-source tools and frameworks

**Deployment Options:**

- Self-hosted VPS (DigitalOcean, Hetzner, Linode)
- On-premise servers
- Docker containers on bare metal
- Kubernetes (optional, only if cost-justified)

## Monitoring and Observability

### Centralized Logging

**ELK Stack (Elasticsearch, Logstash, Kibana):**

- Aggregate logs from all modules
- Structured JSON logging
- Log correlation with the trace id (`traceId`)
- Query and analyze logs in Kibana

### Metrics and Alerting

**Prometheus + Grafana:**

- Collect application metrics (request rate, latency, errors)
- JVM metrics (heap, GC, threads)
- Database connection pool metrics
- Custom business metrics
- Grafana dashboards for visualization
- Alertmanager for critical alerts

## Distributed Tracing

**Jaeger or Zipkin:**

- Trace requests across modules
- Identify performance bottlenecks
- Visualize call graphs
- Measure end-to-end latency
- Debug production issues

**Implementation:**

- OpenTelemetry instrumentation
- Trace context propagation via HTTP headers
- Sample strategically to control storage costs

## Observability

**Signals**

- Logs: JSON on the console in prod. Fields include `traceId`, `spanId`, `userId`. The trace id is the
  only request id: there is no `X-Tracking-ID` or `X-Request-ID`. Logs also go out as OTLP logs when
  export is on.
- Metrics: Micrometer at `/q/metrics` on port 7777. Alloy scrapes it on the private network. The gateway
  does not route it. Every metric has the tags `service` and `env`. `http.server.requests` has latency
  buckets: 5 ms, 10 ms, 25 ms, 50 ms, 100 ms, 250 ms, 500 ms, 1 s, 2.5 s, 5 s, 10 s.
- Traces: OpenTelemetry. The gateway client (`jbh-gateway-client`) makes a client span and sends
  `traceparent`, so the call to `jbh-iam` joins the same trace.

**Env vars**

| Variable | Default | Meaning |
|----------|---------|---------|
| `OTEL_SDK_DISABLED` | `true` (dev: `false`) | Export switch. Stops only the OTLP export. Tracing stays on. Use `true` or `false` only. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:14317` | OTLP gRPC endpoint. `http://jbh-alloy:4317` in Docker. |
| `OTEL_TRACES_SAMPLER_ARG` | `1.0` | Sampling ratio while export is on. |
| `JBH_ENV` | `prod` | Metric tag `env` and `deployment.environment.name`. Dev and test ignore it. |
| `JBH_SERVICE_VERSION` | `unknown` | `service.version` resource attribute. |

**Defaults.** Export is off by default (also in prod, until JBH-84) and on in dev. The dev endpoint
changed from `localhost:4317` to `localhost:14317` (Alloy dev port). To use the LGTM stack directly,
set `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317`.

**Known limit.** With export off, the outgoing `traceparent` has sampled=0. If `jbh-iam` has export on
and this service has it off, the parent-based sampler in `jbh-iam` drops its spans for that request.
In prod all services switch together.

**Privacy rule.** No personal data in logs or in span attributes (e-mail, name, amount, statement
content, free text). Use ids.

The compose env vars land in `jbh-deploy` with JBH-84. Details: `docs/guides/logging.md`.
