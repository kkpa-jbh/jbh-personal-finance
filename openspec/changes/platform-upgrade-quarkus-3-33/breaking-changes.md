# Breaking changes: Quarkus 3.18.4 to 3.33.x

Written by task T1. Only items that touch this repo are kept. Each item names the task that handles it (T2 to T6).
Nothing here was built or run. Facts come from: the Quarkus migration guides 3.19 to 3.33 (GitHub wiki), the
`io.quarkus.platform:quarkus-bom` and `io.quarkus:quarkus-bom` poms from Maven Central, the config model inside the
3.33.4 extension jars, and this repo's poms, `module-info.java`, properties and sources.

## 1. Target version (T1.1)

- Target: `io.quarkus.platform` **3.33.4.1** (newest 3.33.x LTS patch on <https://quarkus.io/releases>, marked LTS).
- Verified on Maven Central (HTTP 200): `quarkus-bom-3.33.4.1.pom`, `quarkus-maven-plugin-3.33.4.1.pom`.
  Other 3.33 versions there: 3.33.1, 3.33.1.1, 3.33.2, 3.33.2.1, 3.33.3, 3.33.3.1 to .3, 3.33.4, 3.33.4.1.
- The platform BOM imports the core BOM `io.quarkus:quarkus-bom:3.33.4`. `3.33.4.1` is a platform micro-patch of core `3.33.4`.
- T2.1: set `<quarkus.version>3.33.4.1</quarkus.version>` (line 289 of `pom.xml`). The same property feeds the BOM import,
  `quarkus-maven-plugin` and the plugin management entry. All three exist for this version.
- Later non-LTS lines exist on Central (3.34 to 3.39). They are not the target (D1).

## 2. Java baseline (T1.4) and Maven requirement

- Java: the 3.33 Getting Started guide requires **JDK 17+**. Quarkus 3.31 added Java 25 support. **Java 21 is fine.**
  The repo uses 21 everywhere (`java.version`, CI `setup-java 21`, `Dockerfile` `eclipse-temurin:21-jre-alpine`).
  Result: D4 is respected, T1.4 does not block.
- Maven: **`quarkus-maven-plugin` 3.33.4.1 declares `requiredMavenVersion` 3.9.12** (read from the plugin's `plugin.xml`;
  the Getting Started guide also says Maven 3.9.12). The repo wrapper uses **3.9.9**
  (`.mvn/wrapper/maven-wrapper.properties`) and the local `mvn` is 3.9.9. The build fails on the plugin until Maven is 3.9.12 or newer.
  `apache-maven-3.9.12-bin.zip` exists on Maven Central (HTTP 200). CI and image workflows both call `./mvnw`.
  - **T6** (new item for T6.2): change `distributionUrl` in `.mvn/wrapper/maven-wrapper.properties` to 3.9.12.
    Tell the user to upgrade the local `mvn` if they run it directly (T8).

## 3. Liquibase checksum risk (T1.5)

- Liquibase: `4.29.1` (Quarkus 3.18.4) to `4.33.0` (Quarkus 3.33.4). Same 4.x line, no major jump.
- `liquibase.ChecksumVersion` in both jars lists V1 to V9, so the latest checksum version is the same (V9).
  Liquibase recomputes checksums with the version stored per row, so existing rows keep using their stored version.
- Change logs: three XML masters (`db/finance-db-master.xml`, `db/preferences-db-master.xml`, `db/notificationdb-master.xml`)
  that include SQL files (`db/changelog/*.sql`, 7 finance, 2 preferences, 1 notification). No `runOnChange`, `validCheckSum`,
  `runAlways`, `splitStatements` or `stripComments` option was found.
- Verdict: **no checksum risk found, T1.5 does not block.** Limit of this check: I compared the checksum version enum,
  not the algorithm bytecode, and I could not read a real `DATABASECHANGELOG`. The proof is T8.3
  (0 errors, 0 new rows on a database that is already migrated). If a "checksum validation failed" error shows up
  there, stop and ask the user (D6). Never edit an applied change set.
- Quarkus keys for Liquibase in use (`liquibase-schema-name`, `change-log`, `migrate-at-start`) exist in 3.33.4.
  Two keys do not (`schemas`, `datasource`), see section 7.

## 4. Library versions managed by the BOM (T1.6, transitive part a)

Versions from the platform BOM `3.18.4` and `3.33.4.1` (core BOM overlaid).

| Library | 3.18.4 | 3.33.4.1 | Effect here |
|---------|--------|----------|-------------|
| Hibernate ORM | 6.6.7 | **7.2.25** | Major. See section 5.1 |
| Jakarta Persistence API | 3.1.0 | **3.2.0** | Part of Hibernate 7. Both have a `module-info`. Section 8 |
| Hibernate Validator | 8.0.2 | **9.1.5** | Major. Jakarta Validation API 3.0.2 to 3.1.1. Section 5.3 |
| Liquibase | 4.29.1 | 4.33.0 | Section 3 |
| PostgreSQL JDBC | 42.7.5 | 42.7.13 | Patch level |
| Agroal (pool) | 2.5 | **3.0.1** | Default pool size changed. Section 5.4 |
| Narayana JTA | 7.1.0 | 7.3.3 | Transactions: no code change expected |
| Jackson (databind) | 2.18.2 | **2.21.7** | Three minors. Section 5.5 |
| Mutiny | 2.8.0 | **3.1.1** | Not used directly here (no `Uni`/`Multi` in sources). Mailer uses it inside |
| Vert.x core | 4.5.13 | 4.5.35 | Patch level |
| Netty | 4.1.118 | 4.1.139 | Patch level |
| SnakeYAML | 2.3 | 2.5 | No YAML config in this repo |
| SmallRye Config | 3.11.2 | 3.16.0 | Same keys |
| SmallRye OpenAPI | 4.0.8 | 4.2.4 | Section 7 |
| MicroProfile OpenAPI API | 4.0.2 | 4.1.1 | `requires org.eclipse.microprofile.openapi` still valid |
| SmallRye Health | 4.1.1 | 4.3.0 | `/q/health` unchanged |
| RESTEasy Reactive / `quarkus-rest` | 3.18.4 | 3.33.x | Section 5.5 |
| Jandex | 3.2.7 | 3.5.3 | Index format reads older indexes. Section 8 |
| Byte Buddy | 1.15.11 | 1.17.8 | Mockito/Hibernate proxies |
| Jakarta WS RS, CDI, Inject, Annotation, Transaction | 3.1.0, 4.1.0, 2.0.1, 3.0.0, 2.0.1 | same | No change |
| Jakarta Mail API / Angus Mail | n/a / 2.0.3 | 2.1.5 / 2.0.5 | Mailer. Section 5.6 |
| JUnit Jupiter | 5.10.5 | **6.0.3** | Major. Section 6 |
| Mockito | 5.15.2 | 5.21.0 | This repo pins 5.10.0. Section 6 |
| REST Assured | 5.5.0 | 5.5.6 | |
| Testcontainers | 1.20.4 | **2.0.4** | Used by Dev Services in `@QuarkusTest`. Section 6 |
| commons-lang3 / commons-io / commons-codec / commons-compress | 3.17.0 / 2.18.0 / 1.17.2 / 1.27.1 | 3.20.0 / 2.21.0 / 1.21.0 / 1.28.0 | POI and PDFBox are managed through these. Section 8 |
| BouncyCastle (bcprov) | 1.79 | 1.86 | Pulled by POI/PDFBox |
| log4j-api | 2.24.3 | 2.25.5 | POI declares it without a version |
| jboss-logmanager | 3.1.1 | 3.2.1 | |
| `quarkus-hibernate-types` (Quarkiverse) | not in BOM | not in BOM | Explicit version needed. Section 5.1 |

## 5. Items by area

### 5.1 Hibernate ORM 7.2 and `quarkus-hibernate-types` (T1.6, D5) - main risk

Facts:
- Quarkus 3.24 moved to Hibernate ORM 7.0; 3.31 to 7.2. Hibernate 7 is mostly compatible with 6.6 but changes
  `UserType`/`JdbcType` SPI signatures and moves JDBC types to internal packages (3.24 guide).
- `io.quarkiverse.hibernatetypes:quarkus-hibernate-types`: **2.2.0 is the newest release on Maven Central** (published 2024-04).
  No 3.x or Hibernate 7 release exists. It is not managed by the Quarkus BOM (0 hits in the platform BOM and the core BOM).
  Its pom depends on `io.hypersistence:hypersistence-utils-hibernate-63:3.7.4` (Hibernate 6.3).
- The extension jar ships its own copy of `io.hypersistence.utils.hibernate.type.json.internal.JsonJdbcTypeDescriptor`
  (patched for Quarkus) plus `QuarkusObjectMapperSupplier`. That copy was compiled against Hibernate 6.x internals,
  so it is very likely to fail on Hibernate 7.2.
- Hypersistence has newer artifacts: `hypersistence-utils-hibernate-70` 3.15.3 and `hypersistence-utils-hibernate-71` 3.16.0
  (its pom lists Hibernate 7.1.11 and 7.2.24). Their `Automatic-Module-Name` is still `io.hypersistence.utils.hibernate.type`.
- Usage in this repo: `@Type(JsonBinaryType.class)` on 6 attributes, with `columnDefinition = "jsonb"`:
  `jbh-finance-infra`: `CategoryJPAEntity` (2: `displayName`, `description`), `ProductJPAEntity.metadata`, `MovementJPAEntity.metadata`;
  `jbh-preferences-infra`: `TeamPreferencesJPAEntity.metadata`, `UserPreferencesJPAEntity.metadata`;
  `jbh-notification-infra`: `NotificationEntity.metadata`. 7 `@Type` lines in 6 files. Poms with the dependency:
  finance-infra, preferences-infra, notification-infra (all `<version>2.2.0</version>`).
  `module-info.java` of finance-infra and preferences-infra: `requires io.hypersistence.utils.hibernate.type;`.
  The only `org.hibernate` import in the sources is `org.hibernate.annotations.Type` (6 files).

Options (decision for T3.2; the leader confirms before T5 edits entities):
- **Option B (recommended): drop the extension and the hypersistence dependency; use Hibernate's own JSON mapping.**
  Replace `@Type(JsonBinaryType.class)` with `@JdbcTypeCode(SqlTypes.JSON)`; keep `@Column(... columnDefinition = "jsonb")`.
  Quarkus wires Hibernate's JSON format mapper to the Quarkus Jackson `ObjectMapper` (the same mapper the extension supplied).
  Removes a dependency, 3 pom entries, and 2 `requires` lines. Risk: JSON value typing for `Map<K,Object>`
  may differ in small ways (numbers). It needs the Dev Services tests plus a manual read of existing rows (T8).
- Option A: replace the extension by `io.hypersistence:hypersistence-utils-hibernate-71:3.16.0` and keep `@Type(JsonBinaryType.class)`.
  No entity edits, but the Quarkus ObjectMapper hook is lost unless a `hypersistence-utils.properties` supplier is added, and
  compatibility of `-71` with ORM 7.2.25 is not proven by me.
- Option C (not recommended): keep 2.2.0 unchanged. It pulls `hypersistence-utils-hibernate-63` next to Hibernate 7. Expect a startup or mapping failure.

Handled by: **T3.1/T3.2** (poms), **T5** (entities, `module-info.java`). Test decision for T5: no new unit test
(entities are in `*-infra`, testing.md); proof is `@QuarkusTest` Dev Services tests in finance-infra and T8.

Other Hibernate ORM 7 items checked against the repo:
- Removed session methods/annotations (`save`, `update`, `@Where`, `@Index`, ...): the repo imports only `org.hibernate.annotations.Type`. Not affected.
- Ambiguous HQL (join without `select`): there is no `join` in any query. Not affected.
- Native queries returning `java.sql` types: the only `createQuery` is JPQL (`MonthlyBalanceJPARepository`). Not affected.
- Basic arrays / `char` columns: no array or `char`/`Character` attributes in the 7 entities. Not affected.
- Misplaced mapping annotations now throw: entities use `@Id`, `@Enumerated`, one `@Convert` (`MonthlyBalanceJPAEntity.period` with
  `YearMonthConverter`, not on `@Id`/`@Version`), `@GeneratedValue` (IDENTITY in `CategoryJPAEntity`; SEQUENCE
  with `@SequenceGenerator(allocationSize = 1)` in `MonthlyBalanceJPAEntity`). Looks fine; T8.3 startup is the proof.
- Pessimistic lock: `ProductRepositoryAdapter` uses Panache `withLock(LockModeType.PESSIMISTIC_WRITE)`. API unchanged. Do not touch (T5.3).
- `getEntityManager().merge(...)` in 4 adapters: still JPA. Not affected.
- Database version check (3.25): Hibernate checks the DB version at start. The PostgreSQL server version must meet the
  Hibernate 7.2 minimum. I did not verify the number. Check in T8.3; if it fails, set `quarkus.datasource.<name>.db-version`.
- `quarkus.hibernate-orm.<name>.database.generation` is deprecated since 3.23 (still accepted in 3.33.4). See section 7 (T4).
- Hibernate ORM now starts even with no entities when its datasource is configured (3.29). `jbh-z-assembly` configures the
  default datasource (`quarkus.datasource.db-kind` and `jdbc.url`) and all entities live in named PUs, so a default PU
  with 0 entities may now start and open an extra pool. See section 7 (T4 item). Check the startup log in T8.3.

### 5.2 Liquibase and named datasources

Covered in section 3. Named datasources and named PUs keep the same key shape in 3.33. Handled by T4 (keys) and T8.3.

### 5.3 Hibernate Validator 9 / Jakarta Validation 3.1

- Used only in `jbh-notification-infra` (`NotificationRestAdapterV1` imports `jakarta.validation`) plus the `quarkus-hibernate-validator` extension.
- Since 3.19, Hibernate ORM + Bean Validation also applies constraints to DDL by default (`quarkus.hibernate-orm.validation.mode`).
  Schema generation is off (`database.generation=none`, Liquibase owns the schema), so no effect. No key in the repo.
- Expression Language impl: `expressly` 5.0.0 to 6.0.0 (managed). Nothing to do.
- Handled by: no task. Verify in T8.5.

### 5.4 JDBC pool default size (Agroal) - capacity risk

- Facts: since 3.31 the default `quarkus.datasource.jdbc.max-size` is **50** (was 20). Confirmed in the 3.33.4 config model
  (default 50). The repo sets no `max-size`.
- This repo has 3 named datasources (`finance`, `preferences`, `notifications`) plus the default one, on the same PostgreSQL,
  with 2 or more instances. Worst case moves from 4 x 20 x N to 4 x 50 x N connections.
- **T4**: set `quarkus.datasource.<name>.jdbc.max-size=20` explicitly in each datasource (keeps the old behavior),
  unless the user prefers 50. Needs the PostgreSQL `max_connections` check (T8).
- `quarkus.datasource.jdbc.enable-metrics` was removed in 3.30. Not used in the repo.

### 5.5 `quarkus-rest` and Jackson

- `quarkus-rest-jackson`: Jackson 2.18.2 to 2.21.7. The 3.21 guide changes `@JsonView` deserialization. The repo has **0** `@JsonView`. Not affected.
- 3.22: REST Data Panache needs `quarkus-rest*` (not RESTEasy classic). Repo uses `quarkus-rest` and no REST Data Panache. Not affected.
- 3.31: security interceptors run before transaction interceptors. The repo has no `@PermissionsAllowed`, `@PermissionChecker` or `@RolesAllowed`. Not affected.
- 3.31: `System.getProperty("quarkus.http.port")`/`test.url` no longer work. Not found in sources (`FinanceDatabaseTestProfile` uses `quarkus.http.test-port=0`). Not affected.
- 3.31: `quarkus.https.port` and `quarkus.https.test-port` removed. Not used.
- 3.28: `quarkus.http.cors` removed (use `quarkus.http.cors.enabled`). Not used in the repo.
- 3.31: TLS registry accepts only TLS 1.3 by default. The repo has no TLS registry config; the mailer uses `quarkus.mailer.tls=true` with port 465 (own TLS config, not the registry). Watch the SMTP connection in T8 only if a named TLS configuration is added.
- `jbh-commons-lib` and `jbh-finance-infra` use Jackson directly (`requires com.fasterxml.jackson.*`). API stable in 2.x. Handled by: T8.5 (same JSON shape).
- Handled by: no code task. T8.5 compares status and JSON shape with 3.18.4.

### 5.6 Mailer, Qute, OpenAPI, Health, Arc

- Mailer (`quarkus-mailer`): keys `mock`, `auth-methods`, `from`, `host`, `port`, `tls`, `username`, `password` exist in 3.33.4. Angus Mail 2.0.3 to 2.0.5. No change expected.
- Qute (`quarkus-qute`): keys `property-not-found-strategy`, `remove-standalone-lines`, `quarkus.default-locale`, `quarkus.locales` exist in 3.33.4. No migration entry for Qute in 3.19 to 3.33 guides. `quarkus.qute.property-not-found-strategy=throw-exception` is strict: a Qute template change in a newer Qute may surface a missing property at render. Check in T8 (send one email in dev with mock).
- Arc: `quarkus.arc.remove-unused-beans`, `quarkus.arc.unremovable-types` exist. No arc migration entry for this range.
- `quarkus.index-dependency.<name>.group-id/artifact-id` exist in 3.33.4. Jandex rules (`docs/guides/jandex-and-modules.md`) unchanged.
- `@Mock` (`io.quarkus.test.Mock`) in `FakeGatewayClientFactory` still exists. `@io.quarkus.test.junit.mockito.InjectMock` was removed in 3.23; the repo has **0** `@InjectMock`. Not affected.

## 6. Test stack: JUnit 6, Mockito, Testcontainers, plugins (T1.3, T3.3)

- 3.31 moved to **JUnit 6** and renamed artifacts: `quarkus-junit5` to `quarkus-junit`, `quarkus-junit5-mockito` to `quarkus-junit-mockito`.
  Relocation poms exist (read: `quarkus-junit5` 3.33.4.1 has a `<relocation>`), so the old names still resolve with a warning.
  **T3**: rename in `jbh-finance-infra/pom.xml` (1), `jbh-notification-infra/pom.xml` (1), `jbh-z-assembly/pom.xml` (2: `quarkus-junit5`, `quarkus-junit5-mockito`).
  Test imports (`io.quarkus.test.junit.QuarkusTest`, `TestProfile`, `QuarkusTestProfile`) keep their packages.
- **Pins that fight the BOM** (to be removed or aligned in T3.3; the leader confirms with `dependency:tree`):
  - Root `pom.xml`: `junit.version` 5.10.2 imported as `junit-bom` *after* `quarkus-bom`. The first import wins, so Quarkus' JUnit 6.0.3 wins and this import is dead. Remove it.
  - `jbh-finance/pom.xml` `dependencyManagement`: explicit `junit-jupiter` `${junit.version}` (5.10.2), `mockito-core` `${mockito.version}` (5.10.0),
    `archunit-junit5` `${archunit.version}` (1.3.0), and `junit-platform-surefire-provider` 1.1.0 (a very old artifact). An explicit entry in a module pom **beats** an imported BOM. Result: the aggregate `junit-jupiter` 5.10.2 next to `junit-jupiter-api`/engine/launcher 6.0.3 in finance modules. Remove the `junit-jupiter` and `mockito-core` pins; the surefire-provider entry is dead since Surefire uses the JUnit Platform.
  - `mockito.version` 5.10.0 is also used in `jbh-z-assembly/pom.xml` (`mockito-core`) and `jbh-preferences-application/pom.xml` (`mockito-junit-jupiter`). BOM value is 5.21.0. Remove those explicit versions.
  - `archunit-junit5` 1.3.0 (explicit in commons-lib, finance-infra, preferences-infra; `archunit.version` in the finance parent). Latest on Central is 1.5.1. I did not verify 1.3.0 against JUnit Platform 6. If ArchUnit tests fail to load, raise it (T3.3).
  - `assertj-core` 3.25.3 explicit in 4 poms. Not in the BOM. Keep.
- Surefire/Failsafe `3.2.5`, Compiler `3.12.1`: the 3.31 guide says: if `<argLine>` is set, add `@{argLine}`; this repo sets **no** `<argLine>` in any pom (grep: none). So no argLine change. JaCoCo (`prepare-agent`) sets `argLine`: check `jbh-finance` `jacoco-maven-plugin` config before changing Surefire. JUnit 6 needs a Surefire that supports Platform 6. Quarkus 3.31+ templates use Surefire 3.5.x (Central has 3.5.0 to 3.5.6, and 3.6.0 notes JUnit 5/6 as supported). **T3.3**: raise `maven-surefire-plugin` and `maven-failsafe-plugin` to the 3.5.x line (suggest 3.5.4 or newer; I did not verify the first 3.5.x with JUnit 6). `useModulePath=true` stays.
- `testcontainers.version` 1.19.5 in root `pom.xml`: **no module uses Testcontainers directly** (no import, no pom use). It is a dead property. BOM manages Testcontainers 2.0.4 for Dev Services. **T3.3**: remove the property (safe) or leave it; no effect.
- `DOCKER_API_VERSION=1.44` in Surefire `environmentVariables` (root pom comment: Testcontainers 1.20 fell back to API 1.32). Testcontainers 2.x may no longer need it. It is harmless: keep it.
- `@QuarkusTest` classes in finance-infra (`ProductRestHttpTest`, `ProcessMovementServiceDatabaseLockTest`) use Dev Services (`FinanceDatabaseTestProfile`).
  They need Docker. The profile sets `quarkus.hibernate-orm.finance.database.generation=none`: rename to `schema-management.strategy` (**T4**, test code, same assertions). All other config overrides there exist in 3.33.
- Mockito: since 3.19 the inline mock maker is the default in `quarkus-junit5-mockito`. Repo tests use plain Mockito and `@InjectMocks` (Mockito's own). No effect.
- `useModulePath`, `module-info.java` in test runs: unchanged by Quarkus.

## 7. `application.properties` keys (T4)

Checked against the 3.33.4 config models and the 3.33 `all-config` page. Files: `jbh-z-assembly`, `jbh-finance-infra`, `jbh-preferences-infra`, `jbh-notification-infra`.

| Key (as used) | Status in 3.33.4 | Action |
|---------------|------------------|--------|
| `quarkus.hibernate-orm.<finance|preferences|notifications>.database.generation` (3 files, `%dev` and `%prod`) | Deprecated since 3.23. Still accepted | Rename to `quarkus.hibernate-orm.<name>.schema-management.strategy=none`. Same meaning. Also in `FinanceDatabaseTestProfile` (T4 / test code) |
| `quarkus.hibernate-orm.finance.schema-management.create-schemas=true` | Valid (new name) | Keep |
| `quarkus.hibernate-orm.<name>.datasource`, `.packages` | Valid | Keep |
| `quarkus.datasource.<name>.*`, `quarkus.datasource.*` | Valid | Keep. Add `jdbc.max-size=20` per section 5.4 |
| default datasource in `jbh-z-assembly` (`db-kind`, `username`, `password`, `jdbc.url`) | Valid. But see 3.29 | Default PU with 0 entities may start. Option: `quarkus.hibernate-orm.active=false` for the default PU. Decide in T4; the leader may ask the user. Verify in T8.3 |
| `quarkus.liquibase.<name>.liquibase-schema-name`, `change-log`, `migrate-at-start` | Valid | Keep |
| `quarkus.liquibase.<name>.schemas`, `quarkus.liquibase.<name>.datasource` | **Not in the 3.33.4 config model.** Likely already ignored in 3.18.4 (I could not fetch the 3.18 reference to prove it) | Do not touch: removing them changes nothing at runtime. Compare the "unrecognized key" list between 3.18.4 and 3.33 in T8.2 |
| `quarkus.smallrye-openapi.scan-classes`, `.auto-add-servers`, `.jwt` | **Not valid keys in 3.33.4 and not in the 3.18.4 deployment jar either** (checked class strings in both). Pre-existing warnings | Do not rename: a rename (`auto-add-server`) would change the output. Note only |
| `quarkus.smallrye-openapi.info-*`, `security-scheme`, `security-scheme-name` | Valid | Keep |
| `quarkus.arc.remove-unused-beans`, `unremovable-types`, `quarkus.index-dependency.*` | Valid | Keep |
| `quarkus.http.port`, `host`, `body.uploads-directory`, `body.delete-uploaded-files-on-end` | Valid | Keep |
| `quarkus.log.category."..".level` | Valid | Keep |
| `quarkus.mailer.*`, `quarkus.qute.*`, `quarkus.default-locale`, `quarkus.locales` | Valid | Keep |
| `quarkus.http.cors` | Not used | none |

Secrets and `${ENV}` references: untouched.

## 8. Transitive dependencies and JPMS (user instruction, T1.7)

### 8.1 (a) What the 3.33 BOM manages differently from 3.18
See the table in section 4. The ones that can reach this repo's code or runtime:
Hibernate ORM 7.2 / Jakarta Persistence 3.2 / Hibernate Validator 9 (Jakarta Validation 3.1), Agroal 3.0 (pool default), Jackson 2.21,
Mutiny 3, Testcontainers 2, JUnit 6, Mockito 5.21, commons-io/lang3/codec/compress, BouncyCastle, log4j-api.
Also changed by Quarkus (3.31): `lz4-java` group id `at.yawk.lz4` (no use here), Apicurio 3, Derby support removed (3.30): none used.

### 8.2 (b) What the repo pins or gets from non-Quarkus dependencies
Check each with the leader's `mvn dependency:tree` (task T3.4).

| Item | Where | Risk |
|------|-------|------|
| `quarkus-hibernate-types` 2.2.0 (explicit) | finance-infra, preferences-infra, notification-infra | High. Section 5.1. It brings `hypersistence-utils-hibernate-63` 3.7.4 and its own `JsonJdbcTypeDescriptor` copy |
| `junit-jupiter` `${junit.version}` 5.10.2, `mockito-core` `${mockito.version}` 5.10.0, `archunit-junit5` 1.3.0, `junit-platform-surefire-provider` 1.1.0 | `jbh-finance/pom.xml` `dependencyManagement` | Medium. Override the BOM (module-level entry beats import). Mix of JUnit 5 and 6 on the test classpath. Section 6 |
| `junit-bom` 5.10.2 import | root `pom.xml`, after `quarkus-bom` | Low. Ignored (first import wins). Remove |
| `mockito-core`/`mockito-junit-jupiter` `${mockito.version}` | `jbh-z-assembly`, `jbh-preferences-application` | Medium. Older than the BOM (5.10.0 vs 5.21.0), with Byte Buddy 1.17.8 |
| `slf4j-api` 2.0.13, `logback-classic`/`logback-core` 1.5.6 (root `dependencyManagement`) | root `pom.xml` | Medium. Explicit entries beat the BOM. slf4j 2.0.13 vs BOM 2.0.17. Quarkus logs through JBoss LogManager; `logback-classic` reaches the app only if a module has it at compile or runtime scope (`jbh-gateway-client` declares `logback-classic` 1.4.11 as runtime, `slf4j-api` 2.0.7). Check that Logback is not on the runtime classpath of the assembly. Existing situation, not new |
| `lombok` 1.18.30 | root `pom.xml` | Low. Java 21 OK |
| `jbh-gateway-client` 1.0.0-SNAPSHOT (from `~/.m2`, `-Pgithub` in CI) | finance-infra, preferences-infra | Medium. Built outside this repo with Jackson (`jackson-core`, `databind`, `jsr310`, `parameter-names` via its own `${jackson.version}`), slf4j 2.0.7. The Quarkus BOM overrides the Jackson versions here (2.21.7). Its jar is built on Java 21. It was compiled with the old Jackson: binary compatible within 2.x. Rebuild is not needed unless `dependency:tree` shows a conflict |
| `jbh-notification-contracts`, `jbh-preferences-contracts` | modules in this repo | Low. No third-party deps in the poms |
| Apache POI 5.4.1 (`poi`, `poi-ooxml`), `poi-ooxml-schemas` 4.1.2 with `xmlbeans` excluded, PDFBox 3.0.8 with `commons-logging` excluded + `commons-logging-jboss-logging` | finance-infra | Medium. POI/PDFBox use `commons-io`, `commons-codec`, `commons-compress`, `commons-collections4`, BouncyCastle, `log4j-api`: all now **BOM-managed to newer versions** (section 4). `poi-ooxml-schemas` 4.1.2 is old next to POI 5.4.1 (5.x does not need it; it was already odd). Exclusions: `xmlbeans` exclusion on 4.1.2 only; POI 5.4.1 still brings `xmlbeans` 5.3.0. `commons-logging` exclusion + `commons-logging-jboss-logging` stays valid |
| `com.opencsv`, `commons-lang3`, `commons-collections4` required in finance-infra `module-info.java` | not declared in any pom of this repo | Transitive. `commons-collections4` comes from POI. The source of `opencsv` and `commons-lang3` is not visible in the poms. The leader should confirm with `dependency:tree`. commons-lang3 is BOM-managed (3.17.0 to 3.20.0) |
| `testcontainers.version` 1.19.5 | root `pom.xml` | Low. Unused property. BOM manages 2.0.4 |
| `assertj-core` 3.25.3 (explicit) | contracts and infra poms | Low. Not in the BOM |

No `<exclusions>` in this repo block a BOM-managed Quarkus artifact. The only exclusions are `xmlbeans` (poi-ooxml-schemas), and `commons-logging` (pdfbox).

### 8.3 (c) JPMS impact (`module-info.java`, `requires`)
Automatic module names come from the jar file name unless the manifest sets `Automatic-Module-Name`.

| `requires` | Module in | Change with 3.33 | Action |
|------------|-----------|------------------|--------|
| `org.hibernate.orm.core` | finance-infra, preferences-infra | `hibernate-core` 7.2.25 manifest still has `Automatic-Module-Name: org.hibernate.orm.core` (read from the jar) | none |
| `jakarta.persistence` | finance-infra, preferences-infra | API 3.2.0 has a `module-info.class` (3.1.0 had one too). Module name stays `jakarta.persistence` | none; watch for split packages with another jar on the module path |
| `jakarta.transaction`, `jakarta.ws.rs`, `jakarta.cdi`, `jakarta.inject`, `jakarta.resource` | infra, assembly | Same API versions (2.0.1, 3.1.0, 4.1.0, 2.0.1, 2.1.0) | none |
| `quarkus.hibernate.orm.panache`, `quarkus.panache.common`, `quarkus.core`, `resteasy.reactive.common`, `smallrye.config.core`, `microprofile.config.api` | infra, assembly | Artifacts still exist in 3.33.4.1 with the same names (`quarkus-hibernate-orm-panache`, `quarkus-panache-common`, `quarkus-core`, `resteasy-reactive-common`, `smallrye-config-core`). Names derive from the file name, so they stay | none. A failure shows at compile time (T8.1) |
| `org.eclipse.microprofile.openapi` | infra | `microprofile-openapi-api` 4.0.2 to 4.1.1 | none expected |
| `io.hypersistence.utils.hibernate.type` | finance-infra, preferences-infra | Same `Automatic-Module-Name` in `-63` 3.7.4 and `-71` 3.16.0. If Option B (section 5.1) is chosen the `requires` line must go (T5) | T5 |
| `org.hibernate.orm.core` + a second Hibernate-related JAR | | If both `-63` and `-71` hypersistence jars were present they would share one automatic module name: split package. Avoid | T3 |
| `com.fasterxml.jackson.core/annotation/databind` | commons-lib, finance-infra, assembly | 2.18.2 to 2.21.x. `jackson-annotations` is `2.21` (no patch), by design | none |
| `org.apache.poi.ooxml`, `org.apache.pdfbox`, `org.apache.commons.lang3`, `org.apache.commons.collections4`, `com.opencsv`, `com.jbh.gateway` | finance-infra, preferences-infra | Non-Quarkus modules: POI 5.4.1, PDFBox 3.0.8 unchanged. Dependencies of POI/PDFBox move to newer BOM versions. Automatic names do not change with version | none |
| Jandex | index files | Jandex plugin 3.1.2 writes the index; Quarkus 3.33 reads Jandex 3.5.3 and older index versions. `quarkus.index-dependency.*` unchanged | optional: raise `jandex-plugin.version` (T3.3); not required |
| Domain/application modules | | No Quarkus or Hibernate imports there. Hexagonal rules unchanged | none |

## 9. Checklist: items that touch this repo, with the handling task

| # | Item | Task |
|---|------|------|
| 1 | `quarkus.version` 3.18.4 to 3.33.4.1 (`pom.xml` line 289) | T2.1 |
| 2 | Maven wrapper 3.9.9 to 3.9.12 (plugin `requiredMavenVersion`) | T6.2 (new file in scope: `.mvn/wrapper/maven-wrapper.properties`) |
| 3 | `quarkus-hibernate-types` 2.2.0 incompatible with Hibernate 7.2 (Option B recommended) | T3.1, T3.2 (poms), T5.1 (6 entities, 2 `module-info.java`) |
| 4 | `quarkus-junit5` and `quarkus-junit5-mockito` renamed to `quarkus-junit`, `quarkus-junit-mockito` (4 dependency lines in 3 poms) | T3 |
| 5 | Remove or align pins: `junit.version`/`junit-bom` import, `jbh-finance/pom.xml` `junit-jupiter`, `mockito-core`, `junit-platform-surefire-provider`; `mockito.version` in `jbh-z-assembly` and `jbh-preferences-application`; `testcontainers.version` | T3.3 |
| 6 | Surefire/Failsafe 3.2.5 to 3.5.x for JUnit 6; ArchUnit 1.3.0 check | T3.3 |
| 7 | `database.generation` to `schema-management.strategy` (3 properties files, test profile) | T4 |
| 8 | JDBC pool default 20 to 50: set `jdbc.max-size` per datasource | T4 |
| 9 | Default PU with 0 entities may start (3.29): decide on `quarkus.hibernate-orm.active=false` | T4 (decision), T8.3 |
| 10 | Unknown keys that pre-exist (`openapi` x3, `liquibase.<name>.schemas/datasource`): leave | T4 (no action) |
| 11 | Hibernate DB-version check vs PostgreSQL server version | T8.3 |
| 12 | `dependency:tree` review of transitive items in section 8 | T3.4 |
| 13 | Docs: `README.md` line 141 says "Quarkus 3.18 is out of community support" | T7.1 |
| 14 | CI Java 21, `Dockerfile` Java 21: no change | T6.1 (confirmed) |

Items checked and **not** affected (no task): `@InjectMock` removal, `@JsonView`, `quarkus.http.cors`, `quarkus.https.port`,
`HttpConfiguration`/legacy config classes (extensions only), Derby, REST Data Panache, security interceptor order, TLS registry,
UBI 9 images (the `Dockerfile` uses `eclipse-temurin`), `quarkus.hibernate-orm.database.generation` removal (still accepted).

## 10. Test decisions for T5 (D7)

Rule: unit test only when a fix changes logic in a domain or application module. Planned T5 fixes touch only `*-infra`
(entities, `module-info.java`) and poms. Result: **no new unit test planned.** Adapted tests: none known; `FinanceDatabaseTestProfile` key rename keeps its behavior.
The existing suite (`mvn clean verify`) and T8 are the proof.

## 11. Changes applied in the first wave (T2, T3, T4, T6)

Nothing was built or run. Versions below were checked on Maven Central.

### T2 - Quarkus version
- `pom.xml`: `quarkus.version` 3.18.4 to 3.33.4.1. `grep 3\.18 --include=pom.xml` finds no other hit.

### T3 - dependencies and pins
- `quarkus-hibernate-types` 2.2.0: **not changed in this wave.** No newer release exists (section 5.1). The user chose Option B
  (`@JdbcTypeCode(SqlTypes.JSON)`). **T5 owns it**: the 6 entities, the 3 pom dependencies and the `requires io.hypersistence.utils.hibernate.type`
  lines in `module-info.java`. Removing the dependency now would break compilation.
- Renamed `quarkus-junit5` to `quarkus-junit` and `quarkus-junit5-mockito` to `quarkus-junit-mockito` in `jbh-finance-infra`, `jbh-notification-infra`
  and `jbh-z-assembly`. Reason: JUnit 6 rename in 3.31. Both artifacts exist at 3.33.4 on Central and `quarkus-junit` is in the BOM.
- Root `pom.xml`: removed the `junit-bom` import and the `junit.version` property (5.10.2). Reason: the Quarkus BOM (JUnit 6.0.3) is imported first and wins; the import was dead.
- `jbh-finance/pom.xml` and `jbh-preferences/pom.xml` (`dependencyManagement`): removed `junit-jupiter` (5.10.2), `mockito-core` (5.10.0) and
  `junit-platform-surefire-provider` (1.1.0). Reason: a module-level entry beats the imported BOM, so they forced JUnit 5 / Mockito 5.10 next to
  JUnit 6 / Mockito 5.21; the provider is dead because Surefire uses the JUnit Platform. The preferences parent had the same three entries
  (section 8.2 named only the finance parent). All users declare `<scope>test</scope>` themselves, so the dropped scope has no effect.
- Removed the `mockito.version` property and its uses in `jbh-z-assembly` and `jbh-preferences-application` (`mockito-core`, `mockito-junit-jupiter`): the BOM now sets 5.21.0.
- Removed the `testcontainers.version` property (1.19.5). No pom or source uses it; Dev Services use the BOM version (2.0.4).
  The `DOCKER_API_VERSION=1.44` Surefire variable stays (harmless).
- `maven-surefire-plugin` and `maven-failsafe-plugin` 3.2.5 to **3.5.4**. Reason: JUnit 6. 3.5.4 exists on Central, and
  `io.quarkus:quarkus-parent:3.33.4` uses `version.surefire.plugin` 3.5.4 for the same reason. No `<argLine>` is set in any pom.
- Not changed: `archunit-junit5` 1.3.0 (explicit in 4 poms + `archunit.version`). Not BOM-managed; I could not prove it loads on JUnit Platform 6.
  If ArchUnit tests fail to load in T8.1, raise it (1.5.1 is the newest I saw). `assertj-core` 3.25.3: not in the BOM, kept.
  JaCoCo 0.8.12, PMD plugin 3.25.0 and SpotBugs: Quarkus does not require a change (not verified further).
- T3.4 is the leader's (`dependency:tree`).

### T4 - application.properties
- `database.generation=none` to `schema-management.strategy=none` for the `finance`, `preferences` and `notifications` PUs (`%dev` and `%prod`),
  and in `FinanceDatabaseTestProfile` (same value, same meaning).
- Added `quarkus.datasource.<name>.jdbc.max-size=20` for `finance`, `preferences`, `notifications` and the default datasource (assembly). Keeps the 3.18 pool size (default is 50 since 3.31).
- `quarkus.hibernate-orm.active=false` for the default PU: **not applied.** My evidence (item 9 above) says only that the default PU "may" start with 0 entities;
  I could not read the 3.29 guide text, and the key was not proven safe. No code injects the default `EntityManager`. Open question for the leader: check the T8.3 startup log
  for an extra default PU or pool, and add the key only if it shows up.
- Left as is, on purpose (item 10): `quarkus.smallrye-openapi.scan-classes`, `auto-add-servers`, `jwt`, and `quarkus.liquibase.<name>.schemas` / `.datasource`.
  Not in the 3.33.4 config model; not in 3.18.4 either for OpenAPI. Removing or renaming them proves nothing without a run, and a rename could change behavior.
  Compare the "unrecognized key" warnings in T8.2.

### T6 - wrapper, CI, image
- `.mvn/wrapper/maven-wrapper.properties`: Maven 3.9.9 to **3.9.12** (URL returns 200 on Central). `distributionSha256Sum` updated to
  `305773a68d6ddfd413df58c82b3f8050e89778e777f3a745c8e5b8cbea4018ef` (I downloaded the zip and computed it; its SHA-512 matches the `.sha512` file on Central).
  The old sum would fail the wrapper download check.
- `ci.yml`, `image.yml`, `publish-contracts.yml` call `./mvnw`, so they pick up 3.9.12. Java 21 in all three, `.github/readme.md` and `Dockerfile` (`eclipse-temurin:21-jre-alpine`): unchanged.
  `Makefile`, `scripts/export-schema.sh`, `dependabot.yml`: no Quarkus, Maven or Java version text. No change.
- Local `mvn` outside the wrapper must be 3.9.12 or newer (tell the user, T8).

## 12. Leader findings from `mvn clean verify` after wave 2 (T3.4, transitive dependencies)

- `maven-enforcer` DependencyConvergence failed in `jbh-finance-infra`: `commons-collections4` came as 4.5.0 (Liquibase 4.33.0 via opencsv) and 4.4 (POI 5.4.1). Fix: pinned 4.5.0 in `jbh-finance/pom.xml` `dependencyManagement`.
- `hypersistence-utils-hibernate-63` 3.7.4 arrives only through `quarkus-hibernate-types`. It is removed together with it in T5.
- JPMS: `smallrye-config-core` 3.16.0 now ships a real `module-info`. The module name changed from `smallrye.config.core` to `io.smallrye.config`. Fixed in the two infra `module-info.java` files. Section 8 above said the names stay; that was wrong for this one. Other `requires` names must be re-checked at compile time.
- Compile now fails only on `org.hibernate.query.BindableType` (removed in Hibernate 7). This confirms the `quarkus-hibernate-types` replacement (D6) in T5.

## 13. T5 applied: replace `quarkus-hibernate-types` (user decision D6, Option B)

Nothing was built or run.

- 6 entities (7 attributes): `@Type(JsonBinaryType.class)` replaced by `@JdbcTypeCode(SqlTypes.JSON)`
  (`org.hibernate.annotations.JdbcTypeCode`, `org.hibernate.type.SqlTypes`). `@Column(... columnDefinition = "jsonb")`
  and the Java field types are unchanged, so the schema and the Liquibase change sets do not change.
  Files: `CategoryJPAEntity` (2), `ProductJPAEntity`, `MovementJPAEntity`, `TeamPreferencesJPAEntity`,
  `UserPreferencesJPAEntity`, `NotificationEntity`.
- Removed the `io.quarkiverse.hibernatetypes:quarkus-hibernate-types` dependency from `jbh-finance-infra`, `jbh-preferences-infra`
  and `jbh-notification-infra` poms. This also drops `hypersistence-utils-hibernate-63` (section 12).
- Removed `requires io.hypersistence.utils.hibernate.type;` from the `module-info.java` of finance-infra and preferences-infra.
  `requires org.hibernate.orm.core;` stays: the new annotations live in `hibernate-core`. Notification-infra has no `module-info.java`.
  No `opens` change: Hibernate reads entities through the Quarkus bytecode setup as before.
- Jackson check: Hibernate 7 JSON uses the Quarkus format mapper, which is backed by the Quarkus `ObjectMapper`
  (the old extension also supplied that mapper). Field types: `Map<String,String>`, `Map<String,Object>`,
  `Map<ProductMetadataKey,Object>`, `Map<MovementMetadataKey,Object>`. All are Maps with String or enum keys and plain values.
  Jackson handles them with no annotation. No POJO is stored. No Jackson annotation needed.
  Residual risk: `Object` values read back as `Integer`/`Long`/`Double`/`String`/`List`/`Map` by Jackson defaults
  (the old type did the same). Check one existing row with metadata in T8.5.
- Search for other uses (`hypersistence`, `hibernate-types`, `JsonBinaryType`) in `*.java`, `pom.xml`: 0 hits.

### T5.4 Test decisions (D7)

- JSON mapping annotation in 6 entities: test needed? **no**. `*-infra`, no logic change (testing.md). Proof: `@QuarkusTest` Dev Services tests and T8.
- Pom dependency removal (3 poms): test needed? **no**. Build change only.
- `module-info.java` `requires` removal (2 files): test needed? **no**. Compile check only.

### T5.6 / T5.7

- Existing tests broken by a Quarkus API change in this task: none found (no test uses `JsonBinaryType` or the removed dependency). No test was changed.
- T5.7 does not apply: T5 had code items (above).

## 14. Leader build results (after wave 3)

`./mvnw -B clean verify` exit 0 (BUILD SUCCESS, all 13 modules, PMD and SpotBugs included). `./mvnw -q -B pmd:check` (Complexity-Check) exit 0. Tests: 0 failures, 0 errors, 0 skipped, including the database-backed tests.
Fixes found only by the build, all JPMS or dependency related:
- `jbh-finance/pom.xml`: pin `commons-collections4` 4.5.0 (enforcer convergence).
- `requires smallrye.config.core` -> `requires io.smallrye.config` (finance-infra, preferences-infra).
- `requires microprofile.config.api` -> `requires org.eclipse.microprofile.config` (z-assembly).
- `jbh-preferences-infra/pom.xml`: add `org.ow2.asm:asm` (BOM version 9.9.1). `io.smallrye.config` requires `org.objectweb.asm` on the module path.
- Stale `jbh-gateway-client` in `~/.m2` (env issue, not the upgrade): the user reinstalled it.
Build log still shows 9 "Unrecognized configuration key" warnings: `quarkus.smallrye-openapi.{scan-classes,jwt,auto-add-servers}` and `quarkus.liquibase.<finance|preferences|notifications>.{schemas,datasource}`. Section 9 found they were not valid in 3.18.4 either. Left unchanged on purpose (no behavior change). The startup check (T8.2/T8.3) stays with the user.
