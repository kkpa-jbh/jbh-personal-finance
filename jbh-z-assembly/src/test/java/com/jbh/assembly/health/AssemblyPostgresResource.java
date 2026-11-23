package com.jbh.assembly.health;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a throwaway PostgreSQL for the whole app (all three datasources) and gives them its URL and
 * login. Same idea as FinancePostgresResource in jbh-finance-infra: a real URL, never an empty one,
 * so Quarkus cannot pick the URL of the properties file (a real database).
 */
public class AssemblyPostgresResource implements QuarkusTestResourceLifecycleManager {

  /** Same major version as production (jbh-deploy compose.yaml). */
  private static final String IMAGE = "postgres:16-alpine";

  private PostgreSQLContainer postgres;

  @Override
  public Map<String, String> start() {
    postgres = new PostgreSQLContainer(IMAGE).withInitScript("db/test-create-assembly-schemas.sql");
    postgres.start();
    final String url = postgres.getJdbcUrl();
    final String user = postgres.getUsername();
    final String password = postgres.getPassword();
    return Map.of(
        "quarkus.datasource.jdbc.url", url,
        "quarkus.datasource.username", user,
        "quarkus.datasource.password", password,
        "quarkus.datasource.finance.jdbc.url", url + "?currentSchema=finance",
        "quarkus.datasource.finance.username", user,
        "quarkus.datasource.finance.password", password,
        "quarkus.datasource.preferences.jdbc.url", url + "?currentSchema=preferences",
        "quarkus.datasource.preferences.username", user,
        "quarkus.datasource.preferences.password", password);
  }

  @Override
  public void stop() {
    if (postgres != null) {
      postgres.stop();
    }
  }
}
