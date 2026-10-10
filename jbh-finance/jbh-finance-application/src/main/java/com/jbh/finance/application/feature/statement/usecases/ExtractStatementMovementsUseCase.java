package com.jbh.finance.application.feature.statement.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;

/**
 * Reads the movements of a bank statement PDF and suggests one finance category per movement.
 *
 * <p><strong>User Explanation:</strong> "Upload a bank statement. We read its movements and
 * suggest a category for each one. Nothing is saved."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>The PDF must already be unlocked; the password never reaches this use case
 *   <li>Money in is classified only with income categories, money out only with expense
 *       categories
 *   <li>A movement without a valid answer gets the Unknown category and is marked for review
 *   <li>A movement under the minimum confidence is marked for review
 *   <li>Movements keep the order of the PDF
 *   <li>The movements are compared with the balance change printed on the statement
 * </ul>
 */
public interface ExtractStatementMovementsUseCase {

  /**
   * Reads the statement, checks its totals and classifies each movement.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command cannot be null
   *   <li>PDF content cannot be empty
   *   <li>The statement must contain at least one movement
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: Active income and expense categories
   *   <li>None persisted: the result is only returned
   * </ul>
   *
   * @param command the unlocked PDF content
   * @return the categorized movements and the totals check
   * @throws BusinessException if no movements are found, or the reader or the classifier fails
   */
  StatementClassificationDTO extract(ExtractStatementMovementsCommand command)
      throws BusinessException;
}
