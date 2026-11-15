# platform-quarkus-runtime Specification

## MODIFIED Requirements

### Requirement: Behavior is unchanged
The upgrade SHALL keep the public HTTP behavior and the database state unchanged.

#### Scenario: Application starts and is healthy
- GIVEN a local PostgreSQL and Consul
- WHEN the application starts
- THEN Liquibase runs the `finance` and `preferences` change logs with no error
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

#### Scenario: Notification module is not part of the application
- GIVEN the application is built from this repository
- WHEN it starts and `GET /jbh-api/notifications` is called
- THEN the response is HTTP 404
- AND Liquibase does not run a `notifications` change log
