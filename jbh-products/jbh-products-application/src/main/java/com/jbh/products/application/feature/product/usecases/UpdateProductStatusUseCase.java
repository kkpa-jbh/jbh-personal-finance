package com.jbh.products.application.feature.product.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.product.commands.UpdateProductStatusCommand;

/**
 * Updates the active/inactive status of a product in the user's portfolio.
 *
 * <p><strong>User Explanation:</strong> "Activate or deactivate a product in your portfolio.
 * Inactive products are hidden from your main view but remain accessible in reports."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Status can be toggled between active and inactive
 *   <li>If product already has the target status, no update is performed
 *   <li>Inactive products are excluded from active product queries
 * </ul>
 */
public interface UpdateProductStatusUseCase {

  /**
   * Updates the active status of the specified product.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command cannot be null
   *   <li>Command fields validation (via command.validate())
   *   <li>Product must exist for the user
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>UPDATE: Product active status flag (if status changes)
   * </ul>
   *
   * @param command contains userId, productId, and desired active status
   * @return updated product DTO
   * @throws BusinessException if product not found or validation fails
   */
  ProductDTO execute(UpdateProductStatusCommand command) throws BusinessException;
}
