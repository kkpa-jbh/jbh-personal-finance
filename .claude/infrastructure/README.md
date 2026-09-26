# Infrastructure Directory

This directory documents technology decisions, deployment strategies, and infrastructure configuration for the JBH Personal Finance application.

## Files

### database.md
Database architecture guidelines including PostgreSQL configuration, schema-per-module approach, natural primary key usage, and database design principles. Reference when creating new schemas or tables.

### deployment.md
Deployment model and constraints. Covers single deployable unit architecture, cost-effective hosting requirements, monitoring setup (ELK, Prometheus/Grafana), and distributed tracing. Use this when planning deployments or infrastructure changes.

### tech-stack.md
Complete technology stack documentation: Quarkus with GraalVM, Maven multi-module setup, JWT security (HMAC, validated by `jbh-gateway`), Consul service discovery, and the API gateway. Reference when adding dependencies or configuring services.

## Technology Decisions

All infrastructure and technology choices documented here are driven by:
- **Budget constraints**: Cost-effective solutions prioritized
- **Single unit deployment**: No cloud provider lock-in
- **Framework agnostic domain**: Business logic independent of infrastructure
- **Native compilation**: GraalVM support for performance

Consult these files before making any infrastructure or technology changes to ensure alignment with project constraints and goals.
