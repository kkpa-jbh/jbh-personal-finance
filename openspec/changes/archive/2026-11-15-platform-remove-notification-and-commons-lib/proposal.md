# Proposal: platform-remove-notification-and-commons-lib

Source ticket: JBH-74 - https://linear.app/kkpa/issue/JBH-74/move-jbh-notification-out-of-jbh-personal-finance-into-its-own
Owner: platform (cross-cutting). Archive domain: `platform`.
Breaking: yes, for the build and the deploy shape. The REST API of finance and preferences does not change.
The app no longer serves `/jbh-api/notifications/**`. That route moves to the new `jbh-notification`
service (JBH-74 section 6). Any build that lists `jbh-notification` or `jbh-commons-lib` as a reactor
module breaks.
Modules affected: root `pom.xml`, `jbh-notification` (deleted), `jbh-commons-lib` (deleted),
`jbh-finance` and `jbh-preferences` (pom only), `jbh-z-assembly` (pom, `application.properties`, README),
`.github/workflows`, `Makefile`, `scripts`, `spotbugs-exclude.xml`, `.coderabbit.yaml`, docs.
Parent: JBH-74, section 5 of the system plan. Sections 1 (publish `jbh-commons-lib`), 2 (new
`jbh-notification` service) and 3 (`jbh-gateway-client`) come first.

## Why

JBH-74 moves notification into its own repository and service. `jbh-commons-lib` moves to its own
repository as a published package. This repo must stop building both, or the code lives in two places.

## What changes

1. Root `pom.xml`: remove the modules `jbh-commons-lib` and `jbh-notification`. Remove the
   `jbh-notification-infra` entry from `dependencyManagement`. Add ONE `jbh-commons-lib` entry
   (`1.0.0-SNAPSHOT`) to the root `dependencyManagement`.
2. `jbh-finance/pom.xml` and `jbh-preferences/pom.xml`: remove their own `jbh-commons-lib` version entries.
3. Root `pom.xml`: add the repository `https://maven.pkg.github.com/kkpa-jbh/jbh-commons-lib` to the
   `github` profile, next to `jbh-gateway-client`. Update the profile comment.
4. Root `pom.xml`: remove `distributionManagement` (nothing deploys a Maven package any more).
5. `jbh-z-assembly`: remove the `jbh-notification-infra` dependency and the `# Notification module`
   index-dependency block of `application.properties`.
6. Delete the folders `jbh-notification/` and `jbh-commons-lib/` (this removes their module-info
   files and `Notification.mk`).
7. Delete `.github/workflows/publish-contracts.yml`.
8. Remove leftovers: `Makefile` targets, the `notification` case of `scripts/export-schema.sh`,
   two `spotbugs-exclude.xml` entries, `.coderabbit.yaml` entries, and the docs and config that name
   the two modules.
9. Update the specs `platform-quarkus-runtime` and `platform-observability` (deltas in this change).

## Why one change and not a roadmap

`config.yaml` says a multi-module change should be a roadmap. The user decided otherwise in the
interview: this is ONE platform change. The deletes and the pom edits must land together, or the
build breaks in between.

## Not included

See design.md, "Not included". Short: the `jbh-deploy` README, the dead preferences lookup code,
a package repository entry for `jbh-notification-contracts`.

## Database and read paths

No database change. No object is created or altered. The `notifications` schema stays in the
`jbh_finance` database and is owned by the new `jbh-notification` service. After this change Liquibase
runs the `finance` and `preferences` change logs only. No hot read path is touched.
