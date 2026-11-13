package com.jbh.finance.application.feature.statement.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.commands.StartStatementImportCommand;
import java.util.UUID;

/**
 * Admits a statement import into a product.
 *
 * <p><strong>User Explanation:</strong> "Upload a bank statement to a product. We accept it if the
 * product can take it and you did not upload the same PDF before."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>The product must belong to the user
 *   <li>Only savings and credit card products accept statements
 *   <li>The same PDF cannot be processing or done twice in one product
 *   <li>A failed import does not block a retry
 *   <li>A processing import older than the stale time counts as failed and is freed
 * </ul>
 */
public interface StartStatementImportUseCase {

  /**
   * Checks the product, frees a stale import of the same PDF and stores a {@code PROCESSING} row.
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>UPDATE: a stale {@code PROCESSING} row of the same PDF becomes {@code FAILED}
   *   <li>INSERT: one {@code PROCESSING} row, atomic against duplicates
   * </ul>
   *
   * @param command the user, product and PDF digest
   * @return the id of the new import
   * @throws BusinessException {@code PRODUCT_NOT_FOUND}, {@code
   *     STATEMENT_PRODUCT_TYPE_NOT_ALLOWED} or {@code STATEMENT_ALREADY_IMPORTED}
   */
  UUID start(StartStatementImportCommand command) throws BusinessException;
}
