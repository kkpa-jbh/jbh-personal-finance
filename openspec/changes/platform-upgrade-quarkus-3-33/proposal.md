# Proposal: platform-upgrade-quarkus-3-33

Source ticket: none
Source plan: `kkpa/plans/architecture/quarkus-3-33-lts-upgrade-2026-10-24.md`
Owner: platform (cross-cutting). Archive domain: `platform`.

## Why

Quarkus 3.18.4 (root `pom.xml`, property `quarkus.version`) is end-of-life. The 3.33 LTS line has
community support until 25 Mar 2027. Staying on 3.18 means no security fixes.

## What changes

- Raise `quarkus.version` to the newest 3.33.x patch (a micro-patch `3.33.x.y` is allowed if it exists).
- Check every dependency that is not managed by the Quarkus BOM (`quarkus-hibernate-types` 2.2.0,
  Testcontainers, Mockito, JaCoCo, PMD, SpotBugs) and raise one only when needed.
- Rename or remove `quarkus.*` keys in the 4 `application.properties` files that are invalid in 3.33.
- Fix code for each breaking change that hits this repo. A refactor is allowed when a Quarkus change needs it, and each one is tied to a named item in `breaking-changes.md`. Hexagonal layers, PMD and SRP rules still apply.
- Adapt tests when a Quarkus API change forces it, and add a unit test when a fix changes logic (see design D7).
- Check CI, image build, `Makefile`, `scripts/` for version-specific text.
- Update docs that name the Quarkus version.

## Goals

- G1: `quarkus.version` is the newest 3.33.x LTS patch.
- G2: All Quarkus-managed and third-party extensions work with 3.33.
- G3: All `application.properties` files use property names valid in 3.33.
- G4: Docs name the new version.

## Out of scope

- Java version change. Java stays 21. If 3.33 needs Java above 21, work stops and the user decides.

## Impact

- Breaking: no API change. HTTP paths, request and response shapes stay the same.
  The change is a build and runtime upgrade, so it affects ALL modules: `jbh-finance`,
  `jbh-notification`, `jbh-preferences`, `jbh-products`, `jbh-commons-lib`, `jbh-z-assembly`.
- Database: no schema change expected. See design.md D6 for the Liquibase risk.
- Hot read paths: none changed on purpose. Hibernate ORM may change after the jump, so the
  finance read paths (monthly balance, movements, products) must be smoke-tested.
- System map (`../jbh-deploy/README.md`): expected no change (it names no Quarkus version).
  Task 7 re-checks it with a grep.
