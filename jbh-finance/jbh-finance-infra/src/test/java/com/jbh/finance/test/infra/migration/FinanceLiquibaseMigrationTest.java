package com.jbh.finance.test.infra.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.jbh.finance.domain.product.vo.ProductMetadataKey;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.infra.adapters.out.persistence.category.CategoryJPAEntity;
import com.jbh.finance.infra.adapters.out.persistence.movement.MovementJPAEntity;
import com.jbh.finance.infra.adapters.out.persistence.product.ProductJPAEntity;
import com.jbh.finance.infra.adapters.out.persistence.product.ProductJPARepository;
import com.jbh.finance.test.infra.movement.FinanceDatabaseTestProfile;
import io.quarkus.agroal.DataSource;
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Checks that the finance Liquibase change log ran well on an empty PostgreSQL: what the {@code
 * finance.databasechangelog} table holds, what is pending, which tables exist, and that the JSON
 * columns are {@code jsonb}. The expected change sets come from parsing the change log files.
 */
@QuarkusTest
@TestProfile(FinanceDatabaseTestProfile.class)
@Tag("database")
class FinanceLiquibaseMigrationTest {

  private static final String NAME = "finance";
  private static final String KEY_SEPARATOR = "|";
  private static final String EXECUTED = "EXECUTED";
  private static final String JSONB = "jsonb";
  private static final String OBJECT = "object";

  // Includes the two Liquibase tables, which live in the same schema.
  private static final Set<String> EXPECTED_TABLES =
      Set.of(
          "categories",
          "products",
          "movements",
          "monthly_balances",
          "databasechangelog",
          "databasechangeloglock");

  private static final String CHANGE_SET_KEYS_SQL =
      "SELECT concat_ws('|', id, author, filename) FROM finance.databasechangelog";
  private static final String EXEC_TYPES_SQL =
      "SELECT DISTINCT exectype FROM finance.databasechangelog";
  private static final String MISSING_CHECKSUMS_SQL =
      "SELECT count(*) FROM finance.databasechangelog WHERE md5sum IS NULL OR md5sum = ''";
  private static final String TABLES_SQL =
      "SELECT table_name FROM information_schema.tables"
          + " WHERE table_schema = 'finance' AND table_type = 'BASE TABLE'";
  private static final String CHANGE_LOG_SCHEMAS_SQL =
      "SELECT table_schema FROM information_schema.tables WHERE table_name = 'databasechangelog'";
  private static final String COLUMN_TYPES_SQL =
      "SELECT concat_ws(':', table_name, column_name, data_type)"
          + " FROM information_schema.columns WHERE table_schema = 'finance'";
  private static final String JSON_TYPE_SQL =
      "SELECT jsonb_typeof(metadata) FROM finance.products WHERE id = ?::uuid";

  @Inject
  @DataSource(NAME)
  javax.sql.DataSource dataSource;

  @Inject
  @LiquibaseDataSource(NAME)
  LiquibaseFactory liquibaseFactory;

  @Inject ProductJPARepository productRepository;

  @Test
  void shouldRecordEveryDeclaredChangeSetOnceInDatabaseChangeLog()
      throws LiquibaseException, SQLException {
    final List<String> declared = declaredChangeSetKeys();

    final List<String> recorded = queryStrings(CHANGE_SET_KEYS_SQL);

    assertFalse(declared.isEmpty(), "The change log files must declare change sets");
    assertEquals(declared.size(), recorded.size());
    assertEquals(Set.copyOf(declared), Set.copyOf(recorded));
  }

  @Test
  void shouldMarkEveryChangeSetAsExecutedWithAChecksum() throws SQLException {
    assertEquals(List.of(EXECUTED), queryStrings(EXEC_TYPES_SQL));
    assertEquals(List.of("0"), queryStrings(MISSING_CHECKSUMS_SQL));
  }

  @Test
  void shouldLeaveNoChangeSetPending() throws LiquibaseException {
    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
      assertEquals(
          List.of(),
          liquibase.listUnrunChangeSets(
              liquibaseFactory.createContexts(), liquibaseFactory.createLabels()));
    }
  }

  @Test
  void shouldCreateOnlyTheExpectedTablesInTheSchema() throws SQLException {
    assertEquals(EXPECTED_TABLES, Set.copyOf(queryStrings(TABLES_SQL)));
  }

  @Test
  void shouldKeepTheChangeLogTableOnlyInTheFinanceSchema() throws SQLException {
    assertEquals(List.of(NAME), queryStrings(CHANGE_LOG_SCHEMAS_SQL));
  }

  @Test
  void shouldStoreEveryJsonMappedColumnAsJsonb() throws SQLException {
    final Set<String> mapped =
        jsonMappedColumns(CategoryJPAEntity.class, ProductJPAEntity.class, MovementJPAEntity.class);
    final Set<String> existing = Set.copyOf(queryStrings(COLUMN_TYPES_SQL));

    final Set<String> notJsonb = new TreeSet<>(mapped);
    notJsonb.removeAll(existing);

    assertFalse(mapped.isEmpty(), "The entities must map JSON columns");
    assertEquals(Set.of(), notJsonb);
  }

  @Test
  void shouldSaveAndReadBackProductMetadataAsJsonObject() throws SQLException {
    final UUID productId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata =
        Map.of(
            ProductMetadataKey.BROKER_NAME, "Test Broker",
            ProductMetadataKey.PAYMENT_DUE_DAY, 15,
            ProductMetadataKey.COMMON_IS_FULLY_WITHDRAWN, true);

    final ProductJPAEntity product = newProduct(productId, metadata);
    QuarkusTransaction.requiringNew().run(() -> productRepository.persist(product));
    final Map<ProductMetadataKey, Object> readBack =
        QuarkusTransaction.requiringNew()
            .call(() -> productRepository.findByProductId(productId).orElseThrow().getMetadata());

    assertEquals(metadata, readBack);
    assertEquals(List.of(OBJECT), queryStrings(JSON_TYPE_SQL, productId.toString()));
  }

  private static ProductJPAEntity newProduct(
      final UUID productId, final Map<ProductMetadataKey, Object> metadata) {
    final ProductJPAEntity product = new ProductJPAEntity();
    product.setId(productId);
    product.setName("migration-test");
    product.setType(ProductType.SAVINGS);
    product.setUserId(UUID.randomUUID());
    product.setMetadata(metadata);
    return product;
  }

  /** Parses the change log files with Liquibase, so no count is written by hand. */
  private List<String> declaredChangeSetKeys() throws LiquibaseException {
    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
      return liquibase.getDatabaseChangeLog().getChangeSets().stream()
          .map(
              changeSet ->
                  String.join(
                      KEY_SEPARATOR,
                      changeSet.getId(),
                      changeSet.getAuthor(),
                      changeSet.getFilePath()))
          .toList();
    }
  }

  /** Reads the {@code JdbcTypeCode(JSON)} fields and builds "table:column:jsonb" entries. */
  private static Set<String> jsonMappedColumns(final Class<?>... entities) {
    final Set<String> columns = new TreeSet<>();
    for (final Class<?> entity : entities) {
      final String table = entity.getAnnotation(Table.class).name();
      Arrays.stream(entity.getDeclaredFields())
          .filter(FinanceLiquibaseMigrationTest::isJsonField)
          .map(field -> String.join(":", table, field.getAnnotation(Column.class).name(), JSONB))
          .forEach(columns::add);
    }
    return columns;
  }

  private static boolean isJsonField(final Field field) {
    final JdbcTypeCode typeCode = field.getAnnotation(JdbcTypeCode.class);
    return typeCode != null && typeCode.value() == SqlTypes.JSON;
  }

  private List<String> queryStrings(final String sql, final String... parameters)
      throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < parameters.length; index++) {
        statement.setString(index + 1, parameters[index]);
      }
      try (ResultSet rows = statement.executeQuery()) {
        final List<String> values = new ArrayList<>();
        while (rows.next()) {
          values.add(rows.getString(1));
        }
        return values;
      }
    }
  }
}
