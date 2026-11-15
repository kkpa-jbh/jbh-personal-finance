# Tasks: platform-remove-notification-and-commons-lib

Source ticket: JBH-74. No test blueprint: this change adds no logic (design.md, Test contract).
Prerequisites for any build: `com.jbh:jbh-commons-lib:1.0.0-SNAPSHOT` and the new-source
`jbh-gateway-client` are in `~/.m2` (JBH-74 sections 1 and 3).

## Group A - Maven build. Depends on: none

- [x] A1 Root `pom.xml`: remove `<module>jbh-commons-lib</module>` and `<module>jbh-notification</module>`.
- [x] A2 Root `pom.xml` `dependencyManagement`: remove `jbh-notification-infra`; add one entry `com.jbh:jbh-commons-lib:1.0.0-SNAPSHOT` (D2).
- [x] A3 `jbh-finance/pom.xml` and `jbh-preferences/pom.xml`: remove the own `jbh-commons-lib` version entries (~line 142 and ~129). The `jbh-*-domain/application/infra` poms stay unchanged.
- [x] A4 Root `pom.xml`: add the `https://maven.pkg.github.com/kkpa-jbh/jbh-commons-lib` repository to the `github` profile; update its comment (D3). Do NOT add a `jbh-notification` repository.
- [x] A5 Root `pom.xml`: remove `distributionManagement` and the comment above it (D4).
- [x] A6 `jbh-z-assembly/pom.xml`: remove the `jbh-notification-infra` dependency.
- [x] A7 `.github/maven-settings.xml`: add <server> id `github-packages-read-commons` (PACKAGES_READ_TOKEN); update the header comment (D3). Depends on: none.
- Acceptance: no `jbh-notification` and no `jbh-commons-lib` module in any reactor `<modules>`; `requires jbh.commons` untouched.

## Group B - Assembly config. Depends on: none

- [x] B1 `jbh-z-assembly/src/main/resources/application.properties`: remove `# Notification module` and the `quarkus.index-dependency.notification-contracts.*` lines. Check for other `notification` keys and report any.
- [x] B2 `jbh-z-assembly/README.md`: remove the notification lines (~56, ~84).
- Acceptance: `grep -i notification jbh-z-assembly` returns nothing in config; port 7777 unchanged.

## Group C - Delete folders and workflow. Depends on: A, B

- [x] C1 Delete the folder `jbh-notification/` (about 72 files, includes `Notification.mk`).
- [x] C2 Delete the folder `jbh-commons-lib/` (about 16 files).
- [x] C3 Delete `.github/workflows/publish-contracts.yml`.
- Acceptance: the three paths do not exist. Do not touch `target/` folders elsewhere.

## Group D - Build tooling leftovers. Depends on: none

- [x] D1 `Makefile`: remove `NOTIFICATION_DIR`, `notification-*` targets, the calls in the all-schema targets, and any `.PHONY` or help entry.
- [x] D2 `scripts/export-schema.sh`: remove the `notification` case.
- [x] D3 `spotbugs-exclude.xml`: remove the two `com.jbh.notification.infra.*` entries.
- [x] D4 `.coderabbit.yaml`: remove `NotificationRoutes`, the `notifications` schema line and the `jbh-notification/jbh-notification-contracts/**` path.
- Acceptance: `make -n` of the remaining targets has no reference to a removed variable; the XML and YAML stay valid.

## Group E - Docs and config leftovers. Depends on: none

- [x] E1 `README.md` (~20, 23, 39-40, 110, 116, 145, 151, 161).
- [x] E2 `.claude/CLAUDE.md` (sync trigger ~76, Modules-Root ~83). Report that `jbh-products` has no folder; do not decide.
- [x] E3 `.claude/infrastructure/database.md`, `.claude/infrastructure/tech-stack.md`, `.claude/skills/jbh-review-business-rules/SKILL.md`.
- [x] E3a Note for E3: commit 3ecb39f already changed one line in `.claude/infrastructure/tech-stack.md` (local Consul). Keep that line; edit only the notification line (~101).
- [x] E4 `docs/guides/{jandex-and-modules,logging,maven-enforcer}.md` and `docs/notes/{API_ROUTING,DATABASE_SCHEMA_EXPORT,api-models-inventory}.md`.
- [x] E5 `jbh-finance/AGENTS.md` (~41): wording only if it says commons-lib is in this repo.
- [x] E6 `openspec/config.yaml` `context`: module list only. Keep the archive domains.
- [x] E7 (optional) Javadoc of `FinanceGlobalExceptionHandler` and `PreferencesGlobalExceptionHandler`: drop "notification" from the module list.
- Acceptance: design.md V5 search shows no unplanned match. `../jbh-deploy/README.md` is not edited.

## Group F - Verification (leader or user). Depends on: A, B, C, D, E

- [x] F1 Run `mvn clean verify` at the root (V1).
- [x] F2 Start the app; check port 7777, `/q/health` UP, Liquibase finance + preferences only, `/jbh-api/notifications` returns 404 (V2 to V4).
- [x] F3 Run the V5 search.
- [x] F4 After merge: first finance CI `image` run (V6). If "could not resolve jbh-notification-contracts", open a follow-up for one `<repository>` entry (R1).

Size estimate: ~111 files (about 89 pure deletes: 72 in `jbh-notification`, 16 in `jbh-commons-lib`, 1 workflow; about 22 edited files), ~340 changed lines (deleted code excluded from the line count; tests and docs included)
