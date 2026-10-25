package com.jbh.notification.infra.migration;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;
import java.util.Map;

/**
 * Boots this module on a throwaway PostgreSQL started by a Testcontainers test resource.
 * Liquibase builds the
 * notifications schema from an empty database, as in production.
 */
public class NotificationDatabaseTestProfile implements QuarkusTestProfile {

  private static final String MAILER_USER = "migration-test@example.com";

  @Override
  public List<TestResourceEntry> testResources() {
    return List.of(new TestResourceEntry(NotificationPostgresResource.class));
  }

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.ofEntries(
        // The container comes from the test resource, so no Dev Services container is wanted.
        Map.entry("quarkus.datasource.devservices.enabled", "false"),
        Map.entry("quarkus.hibernate-orm.notifications.schema-management.strategy", "none"),
        Map.entry("quarkus.http.test-port", "0"),
        // The mailer logger at startup needs an e-mail address with an at sign.
        Map.entry("quarkus.mailer.mock", "true"),
        Map.entry("quarkus.mailer.username", MAILER_USER),
        Map.entry("quarkus.mailer.password", "not-a-real-password"),
        Map.entry("quarkus.index-dependency.notification-contracts.group-id", "com.jbh"),
        Map.entry(
            "quarkus.index-dependency.notification-contracts.artifact-id",
            "jbh-notification-contracts"),
        Map.entry("quarkus.index-dependency.preferences-contracts.group-id", "com.jbh"),
        Map.entry(
            "quarkus.index-dependency.preferences-contracts.artifact-id",
            "jbh-preferences-contracts"));
  }
}
