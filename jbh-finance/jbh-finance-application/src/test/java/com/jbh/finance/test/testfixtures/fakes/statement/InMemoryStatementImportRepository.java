package com.jbh.finance.test.testfixtures.fakes.statement;

import com.jbh.finance.application.feature.statement.dto.NewStatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportStatus;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * In-memory double of the statement import table. It follows the same rules as the database
 * adapter: one non-failed row per product and PDF, and every status change needs a {@code
 * PROCESSING} row. It also records the calls, so a test can check the order and the transaction.
 */
public class InMemoryStatementImportRepository implements StatementImportRepository {

  public static final String TIMED_OUT_CODE = "STATEMENT_IMPORT_TIMED_OUT";
  public static final String MARK_DONE_EVENT = "markDone";

  private final Map<UUID, StatementImportDTO> rows = new LinkedHashMap<>();
  private final List<String> events = new ArrayList<>();
  private final Clock clock;
  private final BooleanSupplier inTransactionProbe;
  private boolean markDoneInTransaction;
  private int interactionCount;

  public InMemoryStatementImportRepository(final Clock clock) {
    this(clock, () -> false);
  }

  /**
   * @param clock the clock that stamps the rows
   * @param inTransactionProbe tells whether a transaction is open when {@code markDone} runs
   */
  public InMemoryStatementImportRepository(
      final Clock clock, final BooleanSupplier inTransactionProbe) {
    this.clock = clock;
    this.inTransactionProbe = inTransactionProbe;
  }

  @Override
  public boolean insertProcessing(final NewStatementImportDTO newImport) {
    interactionCount++;
    final boolean duplicate =
        rows.values().stream()
            .anyMatch(
                row ->
                    row.productId().equals(newImport.productId())
                        && row.pdfSha256().equals(newImport.pdfSha256())
                        && row.status() != StatementImportStatus.FAILED);
    if (duplicate) {
      return false;
    }
    rows.put(
        newImport.id(),
        new StatementImportDTO(
            newImport.id(),
            newImport.userId(),
            newImport.productId(),
            newImport.pdfSha256(),
            StatementImportStatus.PROCESSING,
            null,
            null,
            clock.instant(),
            null));
    return true;
  }

  @Override
  public int markStaleAsFailed(
      final ProductId productId, final PdfSha256 pdfSha256, final Duration staleAfter) {
    interactionCount++;
    final Instant limit = clock.instant().minus(staleAfter);
    int changed = 0;
    for (final StatementImportDTO row : new ArrayList<>(rows.values())) {
      final boolean stale =
          row.status() == StatementImportStatus.PROCESSING
              && row.productId().equals(productId)
              && row.pdfSha256().equals(pdfSha256)
              && row.startedAt().isBefore(limit);
      if (stale) {
        rows.put(row.id(), finished(row, StatementImportStatus.FAILED, TIMED_OUT_CODE, null));
        changed++;
      }
    }
    return changed;
  }

  @Override
  public boolean markDone(final UUID importId, final StatementImportSummaryDTO summary) {
    interactionCount++;
    events.add(MARK_DONE_EVENT);
    markDoneInTransaction = inTransactionProbe.getAsBoolean();
    final StatementImportDTO row = rows.get(importId);
    if (row == null || row.status() != StatementImportStatus.PROCESSING) {
      return false;
    }
    rows.put(importId, finished(row, StatementImportStatus.DONE, null, summary));
    return true;
  }

  @Override
  public boolean markFailed(final UUID importId, final String errorCode) {
    interactionCount++;
    final StatementImportDTO row = rows.get(importId);
    if (row == null || row.status() != StatementImportStatus.PROCESSING) {
      return false;
    }
    rows.put(importId, finished(row, StatementImportStatus.FAILED, errorCode, null));
    return true;
  }

  @Override
  public Optional<StatementImportDTO> findByIdAndProduct(
      final UUID importId, final ProductId productId) {
    interactionCount++;
    return Optional.ofNullable(rows.get(importId))
        .filter(row -> row.productId().equals(productId));
  }

  /** Puts a row as it is, to arrange a test (any status, any start time). */
  public void put(final StatementImportDTO row) {
    rows.put(row.id(), row);
  }

  /** The stored row, or {@code null}. Does not count as an interaction. */
  public StatementImportDTO get(final UUID importId) {
    return rows.get(importId);
  }

  public int rowCount() {
    return rows.size();
  }

  /** Names of the status-change calls, in call order, so a test can check the order. */
  public List<String> events() {
    return events;
  }

  public boolean markDoneRanInTransaction() {
    return markDoneInTransaction;
  }

  /** How many port methods were called. */
  public int interactionCount() {
    return interactionCount;
  }

  private StatementImportDTO finished(
      final StatementImportDTO row,
      final StatementImportStatus status,
      final String errorCode,
      final StatementImportSummaryDTO summary) {
    return new StatementImportDTO(
        row.id(),
        row.userId(),
        row.productId(),
        row.pdfSha256(),
        status,
        errorCode,
        summary,
        row.startedAt(),
        clock.instant());
  }
}
