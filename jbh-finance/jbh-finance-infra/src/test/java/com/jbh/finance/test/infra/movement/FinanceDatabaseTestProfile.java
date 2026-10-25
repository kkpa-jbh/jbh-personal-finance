package com.jbh.finance.test.infra.movement;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Boots this module on a throwaway PostgreSQL started by Quarkus Dev Services. Liquibase builds the
 * schema and the system categories, as in production.
 */
public class FinanceDatabaseTestProfile implements QuarkusTestProfile {

  private static final String DATABASE_USER = "postgres";

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.ofEntries(
        // An empty URL removes the configured one. Dev Services start only when no URL is set.
        Map.entry("quarkus.datasource.finance.jdbc.url", ""),
        Map.entry("quarkus.datasource.finance.devservices.username", DATABASE_USER),
        Map.entry("quarkus.datasource.finance.devservices.password", DATABASE_USER),
        Map.entry(
            "quarkus.datasource.finance.devservices.init-script-path",
            "db/test-create-finance-schema.sql"),
        Map.entry("quarkus.hibernate-orm.finance.schema-management.strategy", "none"),
        // Only the finance datasource is used. This avoids a second, unused container.
        Map.entry("quarkus.datasource.devservices.enabled", "false"),
        Map.entry("quarkus.http.test-port", "0"),
        Map.entry("quarkus.arc.remove-unused-beans", "false"),
        Map.entry("quarkus.index-dependency.finance-domain.group-id", "com.jbh"),
        Map.entry("quarkus.index-dependency.finance-domain.artifact-id", "jbh-finance-domain"),
        Map.entry("quarkus.index-dependency.finance-application.group-id", "com.jbh"),
        Map.entry(
            "quarkus.index-dependency.finance-application.artifact-id", "jbh-finance-application"));
  }
}
