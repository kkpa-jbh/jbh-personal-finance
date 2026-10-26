# platform-quarkus-runtime

## ADDED Requirements

### Requirement: Supported Quarkus version
The service SHALL build and run on the newest Quarkus 3.33.x LTS patch.

#### Scenario: Version pinned once
- GIVEN the root `pom.xml`
- WHEN `quarkus.version` is read
- THEN it equals the chosen 3.33.x patch
- AND no other `pom.xml` hard-codes a Quarkus 3.18 version

### Requirement: Valid configuration
Every `quarkus.*` key in the `application.properties` files SHALL be valid in Quarkus 3.33.

#### Scenario: No unknown or deprecated key
- GIVEN the application starts with the 4 `application.properties` files
- WHEN the startup log is read
- THEN it holds no "Unrecognized configuration key" warning
- AND it holds no deprecation warning for a `quarkus.*` key

### Requirement: Behavior is unchanged
The upgrade SHALL keep the public HTTP behavior and the database state unchanged.

#### Scenario: Application starts and is healthy
- GIVEN a local PostgreSQL and Consul
- WHEN the application starts
- THEN Liquibase runs the `finance`, `preferences` and `notifications` change logs with no error
- AND `GET /q/health` on port 7777 returns status `UP`
- AND the OpenAPI page responds with HTTP 200

#### Scenario: Existing endpoints still answer
- GIVEN a real login through the gateway
- WHEN one finance endpoint and one preferences endpoint are called
- THEN each returns the same status code and JSON shape as before the upgrade

#### Scenario: Java baseline
- GIVEN Quarkus 3.33 requires a Java version above 21
- WHEN the baseline check runs
- THEN work stops and the user decides before any change to CI or the pom

### Requirement: Version in docs
The docs SHALL name the new Quarkus version.

#### Scenario: No stale version text
- GIVEN `README.md`, `docs/`, `.claude/`
- WHEN searched for "3.18"
- THEN there is no match that names the Quarkus version

### Requirement: Tests stay honest
Tests SHALL not be weakened by the upgrade.

#### Scenario: Test adapted for a Quarkus API change
- GIVEN an existing test that no longer compiles after the upgrade
- WHEN it is adapted
- THEN its assertions stay the same

#### Scenario: Code fix changes behavior
- GIVEN a code fix that changes logic in a domain or application module
- WHEN the fix is made
- THEN a unit test for the new behavior is added in the same task
