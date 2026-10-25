package com.jbh.finance.test.infra.movement;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a throwaway PostgreSQL and gives its URL and login to the finance datasource. It sets a
 * real URL instead of an empty one: with Dev Services and an empty override, Quarkus can pick the
 * URL of the properties file, which is a real database.
 */
public class FinancePostgresResource implements QuarkusTestResourceLifecycleManager {

  private static final String IMAGE = "postgres:18";
  private static final String DATASOURCE_PREFIX = "quarkus.datasource.finance.";

  private PostgreSQLContainer postgres;

  @Override
  public Map<String, String> start() {
    postgres = new PostgreSQLContainer(IMAGE).withInitScript("db/test-create-finance-schema.sql");
    postgres.start();
    return Map.of(
        DATASOURCE_PREFIX + "jdbc.url", postgres.getJdbcUrl(),
        DATASOURCE_PREFIX + "username", postgres.getUsername(),
        DATASOURCE_PREFIX + "password", postgres.getPassword());
  }

  @Override
  public void stop() {
    if (postgres != null) {
      postgres.stop();
    }
  }
}
