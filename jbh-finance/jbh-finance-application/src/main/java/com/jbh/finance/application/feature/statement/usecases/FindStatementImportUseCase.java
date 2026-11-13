package com.jbh.finance.application.feature.statement.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.commands.FindStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.StatementImportResultDTO;

/**
 * Finds the state of a statement import.
 *
 * <p><strong>User Explanation:</strong> "Check if your statement is still processing, and see the
 * movements it saved."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>The product must belong to the user
 *   <li>The import must belong to the product
 *   <li>A processing import older than the stale time is reported as failed
 *   <li>Movements are returned only when the import is done, sorted by date
 * </ul>
 */
public interface FindStatementImportUseCase {

  /**
   * Builds the poll answer.
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: the import row, then its movements when {@code DONE}
   * </ul>
   *
   * @param command the user, product and import id
   * @return status, summary and saved movements
   * @throws BusinessException {@code PRODUCT_NOT_FOUND} or {@code STATEMENT_IMPORT_NOT_FOUND}
   */
  StatementImportResultDTO find(FindStatementImportCommand command) throws BusinessException;
}
