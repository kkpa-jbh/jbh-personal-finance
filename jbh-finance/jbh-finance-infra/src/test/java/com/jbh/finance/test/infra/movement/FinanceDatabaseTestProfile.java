package com.jbh.finance.test.infra.movement;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;
import java.util.Map;

/**
 * Boots this module on a throwaway PostgreSQL started by a Testcontainers test resource. Liquibase
 * builds the schema and the system categories, as in production.
 */
public class FinanceDatabaseTestProfile implements QuarkusTestProfile {

  @Override
  public List<TestResourceEntry> testResources() {
    return List.of(new TestResourceEntry(FinancePostgresResource.class));
  }

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.ofEntries(
        // The container comes from the test resource, so no Dev Services container is wanted.
        Map.entry("quarkus.datasource.devservices.enabled", "false"),
        Map.entry("quarkus.hibernate-orm.finance.schema-management.strategy", "none"),
        Map.entry("quarkus.http.test-port", "0"),
        Map.entry("quarkus.arc.remove-unused-beans", "false"),
        Map.entry("quarkus.index-dependency.finance-domain.group-id", "com.jbh"),
        Map.entry("quarkus.index-dependency.finance-domain.artifact-id", "jbh-finance-domain"),
        Map.entry("quarkus.index-dependency.finance-application.group-id", "com.jbh"),
        Map.entry(
            "quarkus.index-dependency.finance-application.artifact-id", "jbh-finance-application"));
  }
}
