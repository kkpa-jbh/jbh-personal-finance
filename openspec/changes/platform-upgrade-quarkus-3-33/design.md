# Design: platform-upgrade-quarkus-3-33

## Context

Root `pom.xml` pins `quarkus.version` 3.18.4 (line 289). It feeds `quarkus-bom` and
`quarkus-maven-plugin` (group `io.quarkus.platform`). The jump crosses 15 minor versions.
The risk is in breaking changes, not in the bump. Config lives in 4 files:
`jbh-finance-infra`, `jbh-preferences-infra`, `jbh-notification-infra`, `jbh-z-assembly`
(`src/main/resources/application.properties`). 10 modules use JPMS (`module-info.java`) and
the Jandex rules from `docs/guides/jandex-and-modules.md` stay as they are.

## Decisions

- D1 Target: newest 3.33.x LTS patch from <https://quarkus.io/releases>. The user chose the LTS line.
- D2 Research first: the breaking-change list is written once, in
  `openspec/changes/platform-upgrade-quarkus-3-33/breaking-changes.md`. Later tasks read it.
  Only items that touch this repo are kept.
- D3 Fixes and refactors: a refactor is allowed when a Quarkus change needs it. Each one is tied to a
  named item in `breaking-changes.md`. Hexagonal layers, PMD and SRP rules (design-principles.md) apply.
- D4 Java baseline: stay on Java 21. If 3.33 needs more, tasks T5 and T6 stop and the user decides.
- D5 Third-party: `quarkus-hibernate-types` (2.2.0) moves to the release that supports 3.33.
  If the BOM manages it, remove the explicit `<version>`. If it is incompatible with the new
  Hibernate ORM major version, replace its usage per its docs and record it.
- D6 Liquibase: a newer Liquibase may change checksums of existing changesets. T1 checks this.
  If a risk exists, stop and ask the user. Never edit an applied changeset.
- D7 Tests only if needed. Existing tests may be ADAPTED when a Quarkus API change forces it; same
  assertions, never weakened (code-quality.md, Delivery Rules). A NEW unit test is added only when a
  T5 code fix changes logic or behavior, in the same task as the fix. Config-key, pom, docs and CI
  edits need no new test. Tests follow `.claude/standards/testing.md`: none for `jbh-*-infra` and
  `jbh-z-assembly` modules. No test blueprint group: the need is unknown until T1 ends.

## Database

No database change. No table, column, index, constraint, view or migration is added or changed.
No new read path. Index plan: not applicable.

## Responsibilities

Fix for breaking-changes 5.1 (D5/D6): JSON columns use Hibernate's own `@JdbcTypeCode(SqlTypes.JSON)`.
No class is new. Each class below only changes the JSON mapping annotation (its job is unchanged).

- `CategoryJPAEntity` (finance-infra): maps the `category` table; `displayName` and `description` are `jsonb`.
- `ProductJPAEntity` (finance-infra): maps the `product` table; `metadata` is `jsonb`.
- `MovementJPAEntity` (finance-infra): maps the `movement` table; `metadata` is `jsonb`.
- `TeamPreferencesJPAEntity` (preferences-infra): maps team preferences; `metadata` is `jsonb`.
- `UserPreferencesJPAEntity` (preferences-infra): maps user preferences; `metadata` is `jsonb`.
- `NotificationEntity` (notification-infra): maps notifications; `metadata` is `jsonb`.
- `module-info.java` of finance-infra and preferences-infra: declare module reads; the
  `io.hypersistence.utils.hibernate.type` line is removed (`org.hibernate.orm.core` stays: it holds
  `JdbcTypeCode` and `SqlTypes`).

## Concurrency

none. The upgrade adds no shared state, cache, lock or executor. The app runs with 2 or more
instances. Observation, not a new risk: during a rolling deploy, an old and a new instance share
one PostgreSQL. Liquibase already uses its `DATABASECHANGELOGLOCK` table, so only one instance
applies change sets. Since no change set is added, the new instance applies nothing.
Two instances changing the same product at the same time: behavior is unchanged
(PostgreSQL row lock and `@Version` in the existing code). T5 must not change those mechanisms.

## Test contract (expected values)

No test is planned before T1. For each T5 fix, the decision "test needed? yes/no" is one line in
`breaking-changes.md`. Rule: a fix that changes logic or behavior in a domain or application
module gets a unit test; every other fix gets none. Where a test is added, the contract is:
the inputs and expected outputs of the changed behavior, written in that line before the code.
Adapted existing tests keep their assertions. The proof otherwise is the existing suite and the
manual checks. Expected results:

| Check | Input | Expected |
|-------|-------|----------|
| Build | `./mvnw clean verify` | exit code 0; all tests pass; PMD and SpotBugs report 0 violations |
| Version | `grep -n "quarkus.version" pom.xml` | one line with the chosen 3.33.x value |
| Stale version | `grep -rn "3\.18" --include=pom.xml .` | no hit that names Quarkus |
| Startup log | start app with local Postgres and Consul | 0 lines "Unrecognized configuration key"; 0 deprecation warnings for `quarkus.*` |
| Liquibase | same start | change logs `finance`, `preferences`, `notifications` run; 0 errors; 0 new rows in `DATABASECHANGELOG` on a database that is already migrated |
| Health | `GET http://localhost:7777/q/health` | HTTP 200, `"status": "UP"` |
| OpenAPI | open the OpenAPI page | HTTP 200 |
| Endpoints | one finance and one preferences call with a real login | same status code and JSON shape as on 3.18.4 |
| CI | push branch, open PR | `ci.yml` green; `image.yml` builds the image |
| Docs | `grep -rn "3\.18\|Quarkus 3" README.md docs .claude` | no hit that names the old version |

## Risks

- Hibernate ORM major change, Hibernate Validator, Liquibase, `quarkus-rest` and Jackson behavior.
  T1 reads the migration guides 3.19 to 3.33 and lists what applies.
- Java baseline above 21: see D4.
- Rollback: revert the branch. No data change, so no data rollback.

## Process rules

- Work on a new branch. `main` is protected: push the branch and open a PR.
- Tests: see D7. Any test follows `.claude/standards/testing.md` (names, folders, `final`, AAA).
- Nobody runs builds or tests in the tasks. The user runs the verification list in tasks.md (T8).
- Follow `.claude/CLAUDE.md`: no framework imports in domain or application, `final` variables,
  `pmd-ruleset.xml`.
