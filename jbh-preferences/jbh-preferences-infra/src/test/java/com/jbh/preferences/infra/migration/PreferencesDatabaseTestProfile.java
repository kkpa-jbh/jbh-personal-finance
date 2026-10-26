package com.jbh.preferences.infra.migration;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;
import java.util.Map;

/**
 * Boots this module on a throwaway PostgreSQL started by a Testcontainers test resource.
 * Liquibase builds the
 * preferences schema from an empty database, as in production.
 */
public class PreferencesDatabaseTestProfile implements QuarkusTestProfile {

  @Override
  public List<TestResourceEntry> testResources() {
    return List.of(new TestResourceEntry(PreferencesPostgresResource.class));
  }

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.ofEntries(
        // The container comes from the test resource, so no Dev Services container is wanted.
        Map.entry("quarkus.datasource.devservices.enabled", "false"),
        Map.entry("quarkus.hibernate-orm.preferences.schema-management.strategy", "none"),
        Map.entry("quarkus.http.test-port", "0"),
        Map.entry("quarkus.index-dependency.preferences-domain.group-id", "com.jbh"),
        Map.entry("quarkus.index-dependency.preferences-domain.artifact-id", "jbh-preferences-domain"),
        Map.entry("quarkus.index-dependency.preferences-application.group-id", "com.jbh"),
        Map.entry(
            "quarkus.index-dependency.preferences-application.artifact-id",
            "jbh-preferences-application"),
        Map.entry("quarkus.index-dependency.preferences-contracts.group-id", "com.jbh"),
        Map.entry(
            "quarkus.index-dependency.preferences-contracts.artifact-id",
            "jbh-preferences-contracts"));
  }
}
