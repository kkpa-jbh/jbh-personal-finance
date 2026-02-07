package com.jbh.products.application.feature.movement.usecases;

import com.jbh.products.application.feature.movement.dto.AddMultipleBasicMovementDTO;
import com.jbh.products.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.util.List;
import java.util.UUID;

/**
 * Imports multiple movements for a product from an uploaded file (typically CSV format).
 *
 * <p><strong>User Explanation:</strong> "Upload your bank statement file to automatically import
 * all transactions into your account. This saves time compared to entering each transaction
 * manually."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>All movements in the file are validated before any are persisted
 *   <li>Account balances are synchronized after all movements are imported
 *   <li>Monthly balance summaries are updated asynchronously
 *   <li>Transaction operates within a Unit of Work (all or nothing)
 * </ul>
 */
public interface AddMovementsUploadedFileUseCase {

  /**
   * Uploads and persists multiple movements from a file in a single transaction.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Movement list cannot be null or empty
   *   <li>Each movement command validated individually (with index for error tracing)
   *   <li>Product must exist for the user
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: Multiple movement records (batch operation within Unit of Work)
   *   <li>UPDATE: Product current balance and net flow
   *   <li>UPDATE: Monthly balance summaries (asynchronous)
   * </ul>
   *
   * @param userId the user who owns the product
   * @param accountId the product ID where movements will be added
   * @param allUploadedMovements list of movement commands parsed from the uploaded file
   * @return DTO containing updated product and list of persisted monthly balances
   * @throws BusinessException if validation fails, product not found, or any movement is invalid
   */
  AddMultipleBasicMovementDTO uploadMovementsFromFile(
      UUID userId, ProductId accountId, List<AddMovementUploadedFileCommand> allUploadedMovements)
      throws BusinessException;
}
