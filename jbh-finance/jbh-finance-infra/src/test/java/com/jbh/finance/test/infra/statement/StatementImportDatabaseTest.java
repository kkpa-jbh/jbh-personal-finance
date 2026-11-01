package com.jbh.finance.test.infra.statement;

import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.FAILED;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.cannedStatement;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.count;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.newPdf;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.string;
import static com.jbh.finance.test.infra.statement.StatementImportTestSupport.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceService;
import com.jbh.finance.application.feature.product.commands.CreateProductCommand;
import com.jbh.finance.application.feature.product.ports.input.CreateProductInputPort;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.commands.RunStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.NewStatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportStatus;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.infra.adapters.in.service.StatementImportRunner;
import com.jbh.finance.test.infra.movement.FinanceDatabaseTestProfile;
import com.jbh.finance.test.infra.statement.StatementImportTestSupport.Categories;
import com.jbh.finance.test.testfixtures.fakes.statement.FakeExtractStatementMovementsUseCase;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Checks the statement import persistence on a real PostgreSQL: the conditional status changes, the
 * rollback when the job finishes after the stale time, the unique index under a race, and the
 * migration. The expected values come from the integration test contract (IT6, IT8, IT11).
 */
@QuarkusTest
@TestProfile(FinanceDatabaseTestProfile.class)
@Tag("database")
class StatementImportDatabaseTest {

  private static final int REPETITIONS = 5;
  private static final int RACING_CALLS = 2;
  private static final long WAIT_SECONDS = 30;
  private static final Duration STALE_AFTER = Duration.ofMinutes(10);
  private static final String TIMED_OUT = BusinessApplicationExceptionType.STATEMENT_IMPORT_TIMED_OUT.name();

  private static final String AGE_ROW_SQL =
      "UPDATE finance.statement_imports SET started_at = now() - interval '11 minutes' WHERE id = ?";
  private static final String MOVEMENTS_OF_IMPORT_SQL =
      "SELECT count(*) FROM finance.movements WHERE statement_import_id = ?";
  private static final String MOVEMENTS_OF_PRODUCT_SQL =
      "SELECT count(*) FROM finance.movements WHERE product_id = ?";
  private static final String CURRENT_BALANCE_SQL =
      "SELECT current_balance::text FROM finance.products WHERE id = ?";
  private static final String ACTIVE_ROWS_SQL =
      "SELECT count(*) FROM finance.statement_imports"
          + " WHERE product_id = ? AND pdf_sha256 = ? AND status <> 'FAILED'";
  private static final String TABLE_EXISTS_SQL =
      "SELECT count(*) FROM information_schema.tables"
          + " WHERE table_schema = 'finance' AND table_name = 'statement_imports'";
  private static final String PARTIAL_UNIQUE_INDEX_SQL =
      "SELECT count(*) FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid"
          + " WHERE c.relname = 'uk_statement_imports_product_pdf_active'"
          + " AND i.indisunique AND i.indpred IS NOT NULL";
  private static final String LINK_COLUMN_SQL =
      "SELECT concat_ws(':', data_type, is_nullable) FROM information_schema.columns"
          + " WHERE table_schema = 'finance' AND table_name = 'movements'"
          + " AND column_name = 'statement_import_id'";

  @Inject StatementImportRepository importRepository;
  @Inject StatementImportRunner runner;
  @Inject FakeExtractStatementMovementsUseCase fakeReader;
  @Inject CreateProductInputPort createProductInputPort;
  @Inject ProcessMonthlyBalanceService processMonthlyBalanceService;
  @Inject UnitOfWork unitOfWork;
  @Inject CategoryQueryRepo categoryRepo;
  @Inject Instance<CategoryService> categoryServices;

  @Inject
  @DataSource("finance")
  javax.sql.DataSource dataSource;

  // IT6
  @Test
  void shouldRefuseToMarkDoneARowThatIsAlreadyFailedAndKeepItUnchanged() throws SQLException {
    final UUID userId = UUID.randomUUID();
    final ProductId productId = createSavingsProduct(userId);
    final UUID importId = insertStaleFailedRow(userId, productId, newPdf());

    final boolean marked = importRepository.markDone(importId, emptySummary());

    assertFalse(marked);
    final StatementImportDTO row = importRepository.findByIdAndProduct(importId, productId).orElseThrow();
    assertEquals(StatementImportStatus.FAILED, row.status());
    assertEquals(TIMED_OUT, row.errorCode());
    assertNull(row.summary());
  }

  // IT6
  @Test
  void shouldRollBackTheMovementsWhenTheJobFinishesAfterTheImportWasMarkedStale()
      throws SQLException, BusinessException {
    final UUID userId = UUID.randomUUID();
    final ProductId productId = createSavingsProduct(userId);
    final byte[] pdf = newPdf();
    final UUID importId = insertStaleFailedRow(userId, productId, pdf);
    final Categories categories =
        StatementImportTestSupport.loadCategories(unitOfWork, categoryRepo, categoryServices);
    fakeReader.returning(cannedStatement(categories));

    runner.run(
        new RunStatementImportCommand(
            importId, userId, productId, new ExtractStatementMovementsCommand(pdf)));
    processMonthlyBalanceService.awaitPendingSync();

    assertEquals(0, count(dataSource, MOVEMENTS_OF_IMPORT_SQL, importId));
    assertEquals(0, count(dataSource, MOVEMENTS_OF_PRODUCT_SQL, productId.value()));
    final BigDecimal balance = new BigDecimal(string(dataSource, CURRENT_BALANCE_SQL, productId.value()));
    assertEquals(0, BigDecimal.ZERO.compareTo(balance), "balance should stay 0 but was " + balance);
    assertEquals(FAILED, string(dataSource, "SELECT status FROM finance.statement_imports WHERE id = ?", importId));
    assertEquals(TIMED_OUT, string(dataSource, "SELECT error_code FROM finance.statement_imports WHERE id = ?", importId));
  }

  // IT8
  @RepeatedTest(REPETITIONS)
  void shouldInsertOnlyOneProcessingRowWhenTwoThreadsInsertTheSamePdfAtTheSameTime()
      throws SQLException, InterruptedException, ExecutionException, TimeoutException {
    final UUID userId = UUID.randomUUID();
    final ProductId productId = createSavingsProduct(userId);
    final PdfSha256 sha = PdfSha256.of(newPdf());

    final List<Boolean> inserted = raceTwoInserts(userId, productId, sha);

    assertEquals(1, inserted.stream().filter(Boolean::booleanValue).count());
    assertEquals(1, count(dataSource, ACTIVE_ROWS_SQL, productId.value(), sha.value()));
  }

  // IT11
  @Test
  void shouldCreateTheStatementImportsTableTheUniquePartialIndexAndTheMovementLink()
      throws SQLException {
    assertEquals(1, count(dataSource, TABLE_EXISTS_SQL));
    assertEquals(1, count(dataSource, PARTIAL_UNIQUE_INDEX_SQL));
    assertEquals("uuid:YES", string(dataSource, LINK_COLUMN_SQL));
  }

  private ProductId createSavingsProduct(final UUID userId) {
    try {
      return createProductInputPort
          .execute(
              new CreateProductCommand(
                  userId, "statement-db-test", ProductType.SAVINGS, ProductMetadata.empty()))
          .id();
    } catch (final BusinessException e) {
      throw new AssertionError("The product could not be created", e);
    }
  }

  /** Inserts a PROCESSING row, ages it past the stale time, and lets the repository fail it. */
  private UUID insertStaleFailedRow(final UUID userId, final ProductId productId, final byte[] pdf)
      throws SQLException {
    final UUID importId = UUID.randomUUID();
    final PdfSha256 sha = PdfSha256.of(pdf);
    assertTrue(importRepository.insertProcessing(new NewStatementImportDTO(importId, userId, productId, sha)));
    update(dataSource, AGE_ROW_SQL, importId);
    assertEquals(1, importRepository.markStaleAsFailed(productId, sha, STALE_AFTER));
    return importId;
  }

  private static StatementImportSummaryDTO emptySummary() {
    return new StatementImportSummaryDTO(StatementImportTestSupport.COP, 0, 0, 0, List.of(), List.of());
  }

  private List<Boolean> raceTwoInserts(
      final UUID userId, final ProductId productId, final PdfSha256 sha)
      throws InterruptedException, ExecutionException, TimeoutException {
    final ExecutorService pool = Executors.newFixedThreadPool(RACING_CALLS);
    try {
      final CountDownLatch ready = new CountDownLatch(RACING_CALLS);
      final CountDownLatch go = new CountDownLatch(1);
      final Callable<Boolean> insert =
          () -> {
            ready.countDown();
            go.await();
            return importRepository.insertProcessing(
                new NewStatementImportDTO(UUID.randomUUID(), userId, productId, sha));
          };
      final Future<Boolean> first = pool.submit(insert);
      final Future<Boolean> second = pool.submit(insert);
      assertTrue(ready.await(WAIT_SECONDS, TimeUnit.SECONDS), "Both threads must be ready");
      go.countDown();
      return List.of(first.get(WAIT_SECONDS, TimeUnit.SECONDS), second.get(WAIT_SECONDS, TimeUnit.SECONDS));
    } finally {
      pool.shutdownNow();
    }
  }
}
