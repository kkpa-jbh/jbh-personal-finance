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
- Log correlation with request IDs
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

## Observability (JBH-60)

- Logs: JSON on the console in prod. Fields include `traceId`, `spanId`, `trackingId`, `userId`.
- Metrics: Micrometer at `/q/metrics` on port 7777. Alloy scrapes it on the private network. The gateway
  does not route it. Every metric has the tags `service` and `env`.
- Traces: OpenTelemetry, off by default. The OTLP endpoint is `jbh-alloy:4317` (gRPC).
  Switch on with `OTEL_SDK_DISABLED=false` and `OTEL_EXPORTER_OTLP_ENDPOINT=http://jbh-alloy:4317`.
- The compose env vars land in `jbh-deploy` with JBH-56.
- Details and the privacy rule: `docs/guides/logging.md`.
