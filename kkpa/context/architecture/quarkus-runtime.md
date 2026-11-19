# platform-quarkus-runtime — maintenance guide

> The map for changing this concept without re-scanning the codebase. Paths + symbols only;
> for current signatures/callers/callees, ask CodeGraph. Pin to file paths, never line numbers.
> All KB links are relative to `kkpa/context/`.

## Glossary

- **Known as:** `Quarkus runtime`, `Quarkus version`, `Quarkus upgrade`, `application startup`
- **Internal name:** `platform-quarkus-runtime`
- **Scope:** cross-cutting. The Quarkus version, the `application.properties` keys, and what the
  assembled application (`jbh-z-assembly`, port 7777) contains at start.

## Component map

Not mapped yet. This guide was built from the spec, which has no file paths.
To add the files, run `/kkpa-context-curate quarkus runtime`. Until then, ask CodeGraph.

## How maintenance works

1. **Version.** `quarkus.version` is set once, in the root `pom.xml`, on the newest 3.33.x LTS patch.
   No other `pom.xml` hard-codes a Quarkus version.
2. **Config.** Every `quarkus.*` key in the `application.properties` files must be valid for that
   version. The startup log shows no "Unrecognized configuration key" and no deprecation warning.
3. **Start check.** With local PostgreSQL and Consul, the app starts on port 7777. Liquibase runs only
   the `finance` and `preferences` change logs. `GET /q/health` returns `UP`. The OpenAPI page answers 200.
4. **Docs.** `README.md`, `docs/` and `.claude/` name the current Quarkus version only.

## Conventions & gotchas

- **Pin the Quarkus version in one place.** Only the root `pom.xml` sets `quarkus.version`.
  _Source: spec platform-quarkus-runtime — Requirement: Supported Quarkus version._
- **A Java baseline above 21 stops the work.** If a new Quarkus needs a newer Java, the user decides
  before any change to CI or the pom.
  _Source: spec platform-quarkus-runtime — Requirement: Behavior is unchanged._
- **An upgrade keeps HTTP behavior and DB state.** One finance and one preferences endpoint keep the
  same status code and JSON shape.
  _Source: spec platform-quarkus-runtime — Requirement: Behavior is unchanged._
- **The notification module is not in this app.** It is the separate `jbh-notification` service.
  Liquibase must not run a `notifications` change log here. The spec expects `GET /jbh-api/notifications`
  to return 404. Known gap (2026-11-15): it returns 500, like every unknown path, because
  `PreferencesGlobalExceptionHandler` wins the `ExceptionMapper<Exception>` collision and has no
  `WebApplicationException` branch. JBH-78 fixes it.
  _Source: spec platform-quarkus-runtime — Requirement: Behavior is unchanged._
- **Tests stay honest in an upgrade.** An adapted test keeps its assertions; a logic fix adds a unit test.
  _Source: spec platform-quarkus-runtime — Requirement: Tests stay honest._

## Related KB

All KB links are relative to `kkpa/context/`.

- Architecture: `architecture/observability.md`
