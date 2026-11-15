# Design: platform-remove-notification-and-commons-lib

Source ticket: JBH-74. Owner: platform. Stack: Quarkus 3.33.x, Maven, Java 21.
This change removes code, build entries and config. It adds no logic.

## Decisions

### D1. One change, deletes and edits together
The module removal, the dependency switch to the external package, and the leftover cleanup land in
one change (user decision). Reason: after the folders go, the root pom, `jbh-finance`,
`jbh-preferences` and `jbh-z-assembly` must already point to the external package, or the reactor
fails.

### D2. `jbh-commons-lib` becomes an external package, declared once
Root `pom.xml` `dependencyManagement` gets one entry:
`com.jbh:jbh-commons-lib:1.0.0-SNAPSHOT`. The two entries with a hard-coded version in
`jbh-finance/pom.xml` and `jbh-preferences/pom.xml` are removed. The users
(`jbh-finance-{domain,application,infra}`, `jbh-preferences-{domain,application,infra}`) keep
their dependency without a version.
Rejected: keep both module-level entries. It repeats the version in two files.
Rejected: a `${jbh-commons-lib.version}` property. One entry, one place, no need yet.

### D3. `github` profile gets only the `jbh-commons-lib` repository
Add `https://maven.pkg.github.com/kkpa-jbh/jbh-commons-lib` next to `jbh-gateway-client` in the
`github` profile (id `github-packages-read` style as the existing entry; the worker copies the
existing entry and changes the url and the id). Update the comment above it: it resolves
`jbh-gateway-client` and `jbh-commons-lib` from GitHub Packages. Local builds use `~/.m2`.
Credentials: `.github/maven-settings.xml` holds a `<server>` only for ids `github` (GITHUB_TOKEN) and
`github-packages-read` (PACKAGES_READ_TOKEN). Maven matches credentials by repository id, and two
repositories in one profile cannot share an id. So the new `jbh-commons-lib` repository gets its own
id, `github-packages-read-commons`, and `maven-settings.xml` gets a matching `<server>` with
username `${env.GITHUB_ACTOR}` and password `${env.PACKAGES_READ_TOKEN}`. Without it CI gets
401 on `jbh-commons-lib`. The header comment of `maven-settings.xml`, which lists the servers, is
updated too. The same PAT secret serves both read ids. This supersedes the "id `github-packages-read`
style" wording above: the worker uses the exact id `github-packages-read-commons`.
Rejected for now: also add a `jbh-notification` repository. See Risks, R1.

### D4. Remove `distributionManagement`
It exists only for `publish-contracts.yml`. `ci.yml` and `image.yml` deploy no Maven package. The
workflow is deleted (D6), so the block is dead. Remove it, and the comment at root `pom.xml` line ~199
("Publishes jbh-notification-contracts ...").

### D5. Assembly drops the notification module
- `jbh-z-assembly/pom.xml`: remove the dependency `jbh-notification-infra`.
- `jbh-z-assembly/src/main/resources/application.properties`: remove the `# Notification module`
  line and the `quarkus.index-dependency.notification-contracts.*` lines (about lines 40-42).
- The `notifications` datasource and Liquibase config live in the `application.properties` of
  `jbh-notification-infra`. They leave with the module. No edit needed in the assembly for them.
- Check the other assembly keys for a `notifications` name (datasource, persistence unit, Liquibase).
  Remove any leftover. Report it if one is found.
- Port stays 7777. `quarkus.http.port` is not touched.

### D6. Delete folders and the publish workflow
Delete `jbh-notification/` (about 72 files, includes `Notification.mk` and its `module-info.java`)
and `jbh-commons-lib/` (about 16 files, includes its `module-info.java`). Delete
`.github/workflows/publish-contracts.yml`. Existing tests inside the folders go with them.
`requires jbh.commons;` in the `module-info.java` files of finance and preferences STAYS. The
external jar keeps the module name `jbh.commons` (JBH-74 section 1 copies `module-info` as is).

### D7. Leftover cleanup list (exact files)
Only the lines that name the removed modules change. Worker rule: grep first, change only matches.
- `Makefile`: `NOTIFICATION_DIR`, the `notification-*` targets, and the calls in the all-schema
  targets. Check that `.PHONY` and the help text do not list them.
- `scripts/export-schema.sh`: the `notification` case.
- `spotbugs-exclude.xml`: the two entries for `com.jbh.notification.infra.*` (lines ~83, ~90).
- `.coderabbit.yaml`: `NotificationRoutes` (~93), the `notifications` schema (~102), the path
  `jbh-notification/jbh-notification-contracts/**` (~133).
- `README.md` (~20, 23, 39-40, 110, 116, 145, 151, 161) and `jbh-z-assembly/README.md` (~56, 84).
- `.claude/CLAUDE.md`: the sync trigger "Change `jbh-notification-contracts`" (~76) and the
  Modules-Root list (~83). The list also names `jbh-products`, which has no folder. The worker
  removes `jbh-notification` and `jbh-commons-lib`, and reports `jbh-products` to the leader without
  deciding.
- `.claude/infrastructure/database.md` (~11), `.claude/infrastructure/tech-stack.md` (~101, the
  gateway no longer routes notifications to this app),
  `.claude/skills/jbh-review-business-rules/SKILL.md` (~73-75).
- `docs/guides/{jandex-and-modules,logging,maven-enforcer}.md`,
  `docs/notes/{API_ROUTING,DATABASE_SCHEMA_EXPORT,api-models-inventory}.md`.
- `jbh-finance/AGENTS.md` (~41): commons-lib stays an allowed import. Change the wording only if it
  says the module is in this repo.
- `openspec/config.yaml` `context`: the module list loses `jbh-notification` and `jbh-commons-lib`.
  The archive domains list is NOT changed (old archives keep their domains).
- Optional: the Javadoc in `FinanceGlobalExceptionHandler` and `PreferencesGlobalExceptionHandler`
  that lists "notification" as a module. Comment-only edit.
- Left alone: `.claude/standards/code-quality.md:256` (unrelated example), `.idea/`, `.codegraph/`,
  `kkpa/plans/`, `openspec/changes/archive/`.

### D8. Spec deltas
- `platform-quarkus-runtime`: scenario "Application starts and is healthy" says Liquibase runs the
  `finance`, `preferences` and `notifications` change logs. MODIFIED to `finance` and `preferences`.
- `platform-observability`: the scenario "E-mail notification" in "No personal data in INFO+ logs"
  describes a behavior that now lives in the `jbh-notification` service. REMOVED from this spec, with
  the reason. The requirement itself stays for the code that remains.
  In OpenSpec delta form, a scenario is removed by a MODIFIED requirement that lists the remaining
  scenarios.

## Test contract

**No unit tests. No test blueprint.** The change adds no logic, and the system plan excludes unit
tests. The existing tests of the deleted folders are deleted with them. The leader marks
`test_first` as skipped.

The verification below is the contract. Inputs and expected results:

| # | Input | Expected result |
|---|-------|-----------------|
| V1 | Prerequisites in `~/.m2`: `com.jbh:jbh-commons-lib:1.0.0-SNAPSHOT` and the new-source `jbh-gateway-client`. Run `mvn clean verify` at the root. | Build SUCCESS. Tests, PMD and SpotBugs green. The reactor lists no `jbh-notification*` and no `jbh-commons-lib`. |
| V2 | Start the app (`mvn -pl jbh-z-assembly quarkus:dev` or the built runner) with local PostgreSQL and Consul. | Starts with no error on port 7777. Liquibase runs the `finance` and `preferences` change logs only. |
| V3 | `GET http://localhost:7777/q/health` | HTTP 200, status `UP`. |
| V4 | `GET http://localhost:7777/jbh-api/notifications/...` (any path) | HTTP 404. No notification resource is registered. |
| V5 | Search the repo (without `archive/`, `.idea/`, `.codegraph/`, `kkpa/plans/`, `target/`) for `jbh-notification`, `jbh-commons-lib` as a module, `notification-contracts`, `NOTIFICATION_DIR`, `publish-contracts`. | No match, except the planned ones: the commons-lib entry in the root `dependencyManagement`, the `github` profile url, `requires jbh.commons`, text that names the external service on purpose. |
| V6 | CI: first finance `image` run after merge (`-Pgithub`). | Resolves all artifacts. See R1 if it fails. |

V1 to V4 need the leader or the user to run them. The worker runs nothing.

## Responsibilities

No new classes. No existing class gets a new job. Two modules are removed.

## Concurrency

none. This change removes code and config. It writes no shared data and adds no async or
parallel code. With 2 instances the behavior is the same as before. The `notifications` schema is
not touched by this app after the change.

## Database

No database change. No table, column, index, constraint, view or migration is created or altered.
The `notifications` schema stays in `jbh_finance`, owned by the new `jbh-notification` service.
Removing the Liquibase changelog of `jbh-notification-infra` from this app does not drop anything:
Liquibase only applies changesets, it never reverses them when a changelog is no longer loaded.
The `databasechangelog` rows of that changelog stay. The new service reads them as its own history,
so JBH-74 section 2 must keep the same changeset ids and file paths (note for that section).

## Risks

- **R1. `jbh-notification-contracts` resolution in CI (known, accepted).** Finance gets
  `jbh-notification-contracts` transitively through `jbh-gateway-client`. Today the reactor builds it.
  After the delete, CI (`-Pgithub`) must download it, and the `github` profile has no url for it
  (user decision: follow the plan, add only `jbh-commons-lib`). Verification: the first finance CI
  `image` run after merge. If it fails with "could not resolve jbh-notification-contracts", the fix is
  one `<repository>` entry in the `github` profile (follow-up change).
- **R2. Build prerequisites.** Local and CI builds fail until `jbh-commons-lib:1.0.0-SNAPSHOT` and the
  new `jbh-gateway-client` are published (JBH-74 sections 1 and 3). Merge this change after them.
- **R3. Missed reference.** A leftover mention in a file not on the D7 list. Mitigation: V5 search.
- **R4. Gateway route.** Until JBH-74 section 6 lands, the gateway still routes notifications to this
  app and gets 404. Order of merge is a leader decision.

## Not included

- `../jbh-deploy/README.md` is NOT edited here. Section 7 of JBH-74 (jbh-leader) owns it. The sync
  rule in `.claude/CLAUDE.md` is met by that section, in a separate commit in that repo.
- The preferences code `jbh-preferences-contracts`, `PreferencesLookupPort` and
  `PreferencesLookupAdapter` (with its test) in `jbh-preferences-infra` stay. After JBH-74 section 2
  they have no caller (the only caller was `DirectPreferencesAdapter` in notification). Removing them
  is a follow-up ticket. The root `dependencyManagement` entry for `jbh-preferences-contracts` stays.
- A `jbh-notification` package repository in the `github` profile (R1).
- Any change to `jbh-products` references beyond reporting them.
- Unit tests (see Test contract).
