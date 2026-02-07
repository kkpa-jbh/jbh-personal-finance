package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.dto.AddBasicMovementDTO;
import com.jbh.products.application.core.vo.commands.AddMovementCommand;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.util.UUID;

/**
 * Adds a single financial movement (transaction) to a product in the user's portfolio.
 *
 * <p><strong>User Explanation:</strong> "Record a transaction in your account, such as a deposit,
 * withdrawal, or expense. The system automatically updates your balances and monthly summaries."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Only certain product types allow adding movements (configured per product type)
 *   <li>Account balances are automatically synchronized after adding movement
 *   <li>Monthly balance reports are updated asynchronously
 * </ul>
 */
public interface AddMovementUseCase {

  /**
   * Adds a movement to the product and synchronizes all related balances.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>User ID cannot be null
   *   <li>Command fields validation (via command.validate())
   *   <li>Product must exist for the user
   *   <li>Product type must allow adding movements
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: New movement record
   *   <li>UPDATE: Product current balance and net flow
   *   <li>UPDATE: Monthly balance summary (asynchronous)
   * </ul>
   *
   * @param userId the user who owns the product
   * @param accountId the product ID where the movement will be added
   * @param movementCommand contains movement details (date, amount, category, metadata)
   * @return DTO containing the created movement and updated product information
   * @throws BusinessException if validation fails or product type doesn't allow movements
   */
  AddBasicMovementDTO addMovement(
      UUID userId, ProductId accountId, AddMovementCommand movementCommand)
      throws BusinessException;
}
