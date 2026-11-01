package com.jbh.finance.application.feature.statement.usecases;

import com.jbh.finance.application.feature.statement.commands.RunStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import java.util.Optional;

/**
 * Runs an admitted statement import: reads the PDF and saves its movements.
 *
 * <p><strong>User Explanation:</strong> "We read your statement in the background and add its
 * movements to the product."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Only the movements in the main currency of the statement are saved
 *   <li>Movements dated after today are not saved
 *   <li>Movements are saved in date order, in one transaction with the end of the import
 *   <li>The funds check is skipped: the bank already accepted the movements
 *   <li>A failure marks the import as failed; it is never rethrown
 * </ul>
 */
public interface RunStatementImportUseCase {

  /**
   * Runs the import and marks it {@code DONE} or {@code FAILED}.
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: the saved movements, with the product balance and monthly balances updated
   *   <li>UPDATE: the import row becomes {@code DONE} or {@code FAILED}
   * </ul>
   *
   * @param command the import id, user, product and the unlocked PDF
   * @return the classification that was read, or empty when the import failed
   */
  Optional<StatementClassificationDTO> run(RunStatementImportCommand command);
}
