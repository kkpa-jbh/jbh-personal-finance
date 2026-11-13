package com.jbh.finance.application.feature.statement.ports.output;

import com.jbh.finance.application.feature.statement.dto.NewStatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import com.jbh.finance.application.feature.statement.vo.PdfSha256;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for the statement import rows. Every status change is one conditional statement on
 * the database, so two instances never both win.
 */
public interface StatementImportRepository {

  /**
   * Inserts a {@code PROCESSING} row, unless the same PDF is already {@code PROCESSING} or {@code
   * DONE} in the product.
   *
   * @param newImport the new row
   * @return {@code true} when the row was inserted, {@code false} for a duplicate
   */
  boolean insertProcessing(NewStatementImportDTO newImport);

  /**
   * Marks as {@code FAILED} (error {@code STATEMENT_IMPORT_TIMED_OUT}) the {@code PROCESSING} rows
   * of the same product and PDF that started more than {@code staleAfter} ago.
   *
   * @param productId the product
   * @param pdfSha256 the PDF digest
   * @param staleAfter the age after which a {@code PROCESSING} row is stale
   * @return the number of rows changed
   */
  int markStaleAsFailed(ProductId productId, PdfSha256 pdfSha256, Duration staleAfter);

  /**
   * Marks a {@code PROCESSING} row as {@code DONE}.
   *
   * @param importId the row
   * @param summary the summary to store
   * @return {@code false} when the row was not {@code PROCESSING} (already {@code FAILED})
   */
  boolean markDone(UUID importId, StatementImportSummaryDTO summary);

  /**
   * Marks a {@code PROCESSING} row as {@code FAILED}.
   *
   * @param importId the row
   * @param errorCode the name of the error
   * @return {@code false} when the row was not {@code PROCESSING}
   */
  boolean markFailed(UUID importId, String errorCode);

  /**
   * Finds one import of a product.
   *
   * @param importId the row
   * @param productId the product that must own the row
   * @return the row, or empty when it does not exist or belongs to another product
   */
  Optional<StatementImportDTO> findByIdAndProduct(UUID importId, ProductId productId);
}
