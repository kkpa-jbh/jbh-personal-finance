# Tasks: platform-upgrade-quarkus-3-33

Owner: platform. No test blueprint group: the test need is unknown until T1 ends (design D7).
Nobody runs a build or tests in T1 to T7. T8 is for the user.

## T1. Collect the breaking changes
Depends on: none. Goals: G2, G3. Files: `openspec/changes/platform-upgrade-quarkus-3-33/breaking-changes.md` (new).
- [x] T1.1 Find the newest 3.33.x LTS patch on <https://quarkus.io/releases> and write it at the top of `breaking-changes.md`.
- [x] T1.2 Read the migration guides 3.19 to 3.33 (<https://github.com/quarkusio/quarkus/wiki/Migration-Guides>). Use Context7 (`/websites/quarkus_io_guides`) and <https://quarkus.io/guides/update-quarkus> if the wiki is hard to read.
- [x] T1.3 Write a checklist with only the items that touch this repo: the extensions in use (`quarkus-arc`, `quarkus-rest`, `quarkus-rest-jackson`, `quarkus-smallrye-openapi`, `quarkus-smallrye-health`, `quarkus-hibernate-orm-panache`, `quarkus-hibernate-validator`, `quarkus-jdbc-postgresql`, `quarkus-liquibase`, `quarkus-mailer`, `quarkus-qute`, `quarkus-junit5`, `quarkus-junit5-mockito`), `quarkus.*` properties, JPMS/Jandex, Liquibase, named persistence units, Qute, mailer, OpenAPI, test support. Mark each item with the task that handles it (T2 to T6).
- [x] T1.4 State the Java baseline of 3.33. If it is above 21, STOP and ask the user (D4).
- [x] T1.5 State whether the Liquibase version change affects checksums of existing change sets (D6). If yes, STOP and ask the user.
- [x] T1.6 Note Hibernate ORM, Hibernate Validator, `quarkus-rest` and Jackson changes, and the supported release of `quarkus-hibernate-types`.
- [x] T1.7 Add a section "Transitive dependencies" to `breaking-changes.md` (user instruction): (a) libraries the 3.33 BOM manages differently from 3.18 (Hibernate, Jackson, Liquibase, Jakarta APIs, Mutiny/Vert.x, Netty, JDBC driver, SnakeYAML); (b) versions this repo pins or gets from non-Quarkus dependencies that could conflict or be ignored; (c) JPMS impact (`requires`, automatic-module names). Done: section 8 of `breaking-changes.md`.

## T2. Bump the Quarkus version
Depends on: T1. Goals: G1. Files: `pom.xml`.
- [x] T2.1 Change `<quarkus.version>3.18.4</quarkus.version>` to the patch from T1.1. Change nothing else in that property block.
- [x] T2.2 Run `/usr/bin/grep -rn "3\.18" --include=pom.xml .` and fix any hit that names Quarkus.

## T3. Check non-managed dependency versions
Depends on: T1, T2. Goals: G2. Files: `pom.xml`, `jbh-finance/jbh-finance-infra/pom.xml`, `jbh-notification/jbh-notification-infra/pom.xml`, `jbh-preferences/jbh-preferences-infra/pom.xml`.
- [x] T3.1 Set `io.quarkiverse.hibernatetypes:quarkus-hibernate-types` (now 2.2.0) to the release that supports 3.33. If the BOM manages it, remove the explicit `<version>`. Explain why a dependency changes (code-quality.md, Delivery Rules).
- [x] T3.2 If the extension is incompatible with the Hibernate ORM version, replace its usage per its docs. Record this in `breaking-changes.md`. If the replacement needs code in several modules, add it under T5.
- [x] T3.3 Check `testcontainers.version` (1.19.5), `mockito.version` (5.10.0), JaCoCo, PMD and SpotBugs plugin versions. Raise one only when T1 or the Quarkus BOM requires it. Write the reason in `breaking-changes.md`.
- [ ] T3.4 Check transitive dependencies (user instruction). After the bump, the leader runs `mvn dependency:tree` and `mvn dependency:tree -Dverbose` per module and compares with the T1.7 list. Look for: version conflicts, an explicit `<version>` or exclusion that now overrides or blocks a BOM-managed version, duplicate or removed artifacts, and changed Jakarta or Hibernate versions. Fix in the pom and write each finding in `breaking-changes.md`.

## T4. Update `application.properties` keys
Depends on: T1. Goals: G3. Files: the 4 `application.properties` (finance-infra, preferences-infra, notification-infra, z-assembly).
- [x] T4.1 Compare each `quarkus.*` key with the 3.33 config reference (<https://quarkus.io/guides/all-config>, via Context7) and the T1 list.
- [x] T4.2 Check first: `quarkus.datasource.<name>.*`, `quarkus.hibernate-orm.<name>.*`, `quarkus.liquibase.<name>.*`, `quarkus.index-dependency.*`, `quarkus.arc.*`, `quarkus.http.body.*`, `quarkus.mailer.*`, `quarkus.qute.*`, `quarkus.smallrye-openapi.*`, `quarkus.log.category.*`.
- [x] T4.3 Rename or remove only the keys that changed. Keep meaning and values. Do not touch secrets or `${ENV}` references.

## T5. Fix source code for breaking changes
Depends on: T1, T2, T3. Goals: G2. Files: decided by the T1 list (tests only when D7 says so; likely `module-info.java`, `*-infra` Panache/Hibernate entities, REST adapters, Qute templates, `@QuarkusTest` classes).
- [x] T5.1 For each T1 item marked "T5", find usages with CodeGraph (`codegraph_search`, `codegraph_impact`) and edit. A refactor is allowed if the Quarkus change needs it (D3). Tie each one to a named item in `breaking-changes.md`.
- [x] T5.2 Keep the hexagonal rules: no framework import in domain or application. Respect `pmd-ruleset.xml` ("Common traps" in code-quality.md).
- [x] T5.3 Do not touch row lock or `@Version` code paths unless a break forces it. Never add in-JVM state.
- [x] T5.3a For every new or changed class, add one line (class, its one job) to the Responsibilities section of design.md.
- [x] T5.4 Decide per fix: test needed? yes/no. Write one line per fix in `breaking-changes.md`. Yes only if the fix changes logic or behavior in a domain or application module (D7).
- [x] T5.5 For each "yes", add the unit test in the same task as the fix, per `.claude/standards/testing.md`. No test for `jbh-*-infra` or `jbh-z-assembly`.
- [x] T5.6 If an existing test breaks because of a Quarkus API change, adapt it. Keep its assertions. Never weaken or delete it. If it looks wrong, report `blocked`.
- [x] T5.7 If the T1 list holds no code item, tick T5 and write "no code change needed" in `breaking-changes.md`.

## T6. Check CI, image and scripts
Depends on: T1, T2. Goals: G2. Files: `.github/workflows/ci.yml`, `.github/workflows/image.yml`, `Makefile`, `scripts/`, `jbh-z-assembly/Dockerfile` (change only if version-specific).
- [x] T6.1 Confirm Java 21 still works for 3.33 (T1.4). If not, STOP and ask the user.
- [x] T6.2 Search `Makefile`, `scripts/`, `jbh-z-assembly/Dockerfile`, `src/main/docker` (if present) and the workflows for Quarkus version or base-image text. Fix only version-specific text.

## T7. Update docs and close the plan
Depends on: T2, T3, T4, T5, T6. Goals: G4. Files: `.claude/infrastructure/tech-stack.md`, `README.md`, `docs/`, `.claude/CLAUDE.md` (only where they name the version), the source plan file.
- [x] T7.1 Run `/usr/bin/grep -rn "3\.18\|Quarkus 3" README.md docs .claude` and replace the old version with the new one.
- [x] T7.2 Grep `../jbh-deploy/README.md` for a Quarkus version. Edit only if a line is wrong.
- [ ] T7.3 At the end of the work, the leader or user moves the plan to `kkpa/plans/executed/architecture/` and adds its Execution Notes (as the plan asks).

## T8. User verification (not run by agents)
Depends on: T7.
- [ ] T8.1 `./mvnw clean verify` passes (tests, PMD, SpotBugs).
- [ ] T8.2 Build log has no "unrecognized configuration key" or deprecation warning. Fix the key in the matching file if it has.
- [ ] T8.3 Start the app with local Postgres and Consul (`../jbh-deploy/README.md`, "Start order"). Liquibase runs the 3 change logs with no error.
- [ ] T8.4 `http://localhost:7777/q/health` returns UP. The OpenAPI page responds.
- [ ] T8.5 One finance endpoint and one preferences endpoint work through the gateway with a real login.
- [ ] T8.6 Push the branch, open a PR, `ci.yml` is green. `image.yml` builds the image. Deploy to a test environment before production.

## Parallel plan

T1 first. Then T2 and T4 in parallel (disjoint files). T3 after T2 (shares `pom.xml`). T6 after T2 (disjoint from T3/T4). T5 after T3 (may touch poms and code). T7 after T2-T6. T8 last.

Size estimate: 14-35 files, ~250-900 changed lines (tests and docs included). This is an estimate; T1 decides the real size.
