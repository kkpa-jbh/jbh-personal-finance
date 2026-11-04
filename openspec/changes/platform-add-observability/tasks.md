# Tasks: platform-add-observability

Source ticket: JBH-60. Owner: platform. Rules: design.md decisions D1 to D12 bind every task.
Test execution: the worker runs nothing. The leader runs the build and the suite after each wave.
Paths: `APP`, `DOM`, `NOT` are defined in design.md D12. `ASM` = `jbh-z-assembly`.
`INFRA` = `jbh-finance/jbh-finance-infra`.

## Group T - Test blueprint (Test-First). Depends on: none

- [x] T1 Write `ASM/src/test/java/com/jbh/assembly/config/MetricsTagsConfigTest.java`: the 4 tests of the design.md contract (`SimpleMeterRegistry`, `MeterFilter`).
- [x] T2 Write `ASM/src/test/java/com/jbh/assembly/config/ObservabilityConfigPropertiesTest.java`: the 10 tests of the contract. Load `src/main/resources/application.properties` by relative path with `java.util.Properties`.
- [x] T3 Skeleton so T1 compiles: `ASM/src/main/java/com/jbh/assembly/config/MetricsTagsConfig.java` (constructor with `service` and `env`, `commonTags()` returns `MeterFilter`, body throws `UnsupportedOperationException("not implemented")`, no CDI logic yet), plus the dependency `io.quarkus:quarkus-micrometer-registry-prometheus` in `ASM/pom.xml` and `requires micrometer.core;` in `ASM/src/main/java/module-info.java` (VERIFY the name).
- Acceptance: both test classes compile; `MetricsTagsConfigTest` fails with "not implemented"; `ObservabilityConfigPropertiesTest` fails on missing keys; `ConsulServiceRegistrationTest` still passes.

## Group A - Runtime config and dependencies (jbh-z-assembly). Depends on: T

- [x] A1 `ASM/pom.xml`: add `quarkus-logging-json`, `quarkus-opentelemetry`, and `quarkus-observability-devservices-lgtm` with `<scope>provided</scope>` (no versions, the BOM manages them). Keep the existing deps. (`quarkus-micrometer-registry-prometheus` is added in T3.)
- [x] A2 `ASM/src/main/resources/application.properties`: apply D1, D2, D3, D4 (`quarkus.datasource.metrics.enabled`), D5 (`jbh.metrics.env` per profile), D8 (`quarkus.observability.enabled=false`). Add `quarkus.application.name=jbh-personal-finance`. Put the new keys in an "OBSERVABILITY" section with a short comment per key. Resolve the VERIFY items; if a key name differs, use the documented one and tell the leader.
- [x] A3 Implement `MetricsTagsConfig` (D5): `@Singleton`, constructor injection with `@ConfigProperty`, `@Produces @Singleton MeterFilter commonTags()` returning `MeterFilter.commonTags(Tags.of("service", service, "env", env))`. Final fields. No logic besides that.
- Acceptance: T1 and T2 tests pass; no key in `application.properties` is unknown or deprecated in 3.33 (startup log); `ConsulServiceRegistration` untouched.

## Group B - AI spans (jbh-finance-infra). Depends on: T (A is not needed: different files)

- [x] B1 `INFRA/pom.xml`: add `io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations` and `io.opentelemetry:opentelemetry-api` (no versions). `INFRA/src/main/java/module-info.java`: add the `requires` lines (VERIFY the automatic module names).
- [x] B2 Add `@WithSpan("<name>")` on `AzureLayoutTextSourceAdapter.extractText`, `OpenRouterLlmAdapter.answer`, `GeminiLlmAdapter.answer`, `JevMovementClassifierAdapter.classify` with the span names of D6. No `@SpanAttribute`. No other change in these files.
- [x] B3 `StatementImportDispatcher.dispatch`: wrap the task with `Context.current().wrap(...)` (D7). No other change.
- Acceptance: the 4 methods carry the annotation; no OTel type appears in `APP` or `DOM`; the ArchUnit tests still pass; check I5 and I6 of design.md when the leader can run them.
- Depends on: T (and B1 before B2 and B3, same group).

## Group C - Log audit (INFO/WARN/ERROR lines). Depends on: T. The 5 sub-tasks touch different files and run in parallel.

Each task changes only the log arguments and the matching `{}` of the lines in the design.md D12 table. Keep the level and
the sentence. Add no log line. Keep variables `final`. Remove any import that becomes unused (PMD `UnnecessaryImport`).

- [x] C1 `APP/async/AsyncTaskExecutorImpl.java` (63, 71, 127), `APP/feature/monthlybalance/services/ProcessMonthlyBalanceServiceImpl.java` (284) and `jbh-finance/jbh-finance-infra/src/main/java/com/jbh/finance/infra/common/logging/LoggingFilter.java` (47: drop the client IP argument and "from IP: {}"; if `getClientIP` is then unused, remove it so PMD `UnusedPrivateMethod` stays green).
- [x] C2 Product: `EditProductInputPort:55`, `UpdateProductStatusInputPort:36, 50`, `DeleteProductInputPort:40`, `CreateProductInputPort:49`, `ProductLifecycleServiceImpl:92, 115, 167, 197`.
- [x] C3 Movement: `LiquidateProductInputPort:74`, `AddTransferJbhProductsInputPort:56, 90, 94`, `ProcessMovementServiceImpl:89, 121, 163, 182, 278, 281`.
- [x] C4 Domain: `MoneyGrowthCalculator:58`, `MonthlyBalanceDomain:159`, `ProductDomain:299`. (Domain stays framework-free: SLF4J only.)
- [x] C5 Notification: `QuarkusMailerAdapter:35, 52, 69, 75`, `NotificationService:67, 123, 148`, `MailerConfigLogger:56, 58`.
- Acceptance (all C): the I9 grep of design.md finds none of the listed names; no existing test changed; if an existing test asserts a changed log text, stop and report `blocked` with the test name.

## Group D - Cleanup and docs. Depends on: A (D2 needs the final key names)

- [ ] D1 Delete `INFRA/src/main/resources/logback-spring.xml` (D9). First grep the repo for other references (`logback-spring`); only `docs/guides/logging.md` is expected.
- [ ] D2 Rewrite the Logback part of `docs/guides/logging.md`: JSON in prod, MDC fields, `traceId`/`spanId`, the privacy rule (what never goes in a log: amounts, balances, statement or OCR text, LLM prompts or answers, IBAN or account numbers, e-mail addresses, client IP address, API keys, transaction lists, whole domain objects), the env vars (`OTEL_SDK_DISABLED`, `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_TRACES_SAMPLER`, `OTEL_TRACES_SAMPLER_ARG`, `QUARKUS_OBSERVABILITY_ENABLED`), `/q/metrics`.
- [ ] D3 Add a short "Observability" paragraph to `.claude/infrastructure/deployment.md`: `/q/metrics` on port 7777 for Alloy, the OTLP endpoint `jbh-alloy:4317`, and that the compose env vars land in `jbh-deploy` with JBH-56.
- Acceptance: no doc names `logback-spring.xml` as a live config.

## Group E - Verification (leader or user). Depends on: A, B, C, D

- [ ] E1 Leader runs `mvn clean verify` (tests, PMD, SpotBugs). Compile errors from JPMS `requires` or a wrong key are fixed by the owner of the group.
- [ ] E2 Run integration checks I1 to I9 of design.md. I2, I5 and I6 need a collector or Docker. Report any miss.

Size estimate: 33 files, ~520 changed lines (tests and docs included)
