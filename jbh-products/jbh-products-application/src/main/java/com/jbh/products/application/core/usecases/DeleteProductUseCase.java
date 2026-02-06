package com.jbh.products.application.core.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.vo.commands.DeleteProductCommand;

/**
 * Soft deletes a financial product from the user's portfolio.
 *
 * <p><strong>User Explanation:</strong> "Remove a product from your portfolio. The product will be
 * archived and hidden from your active accounts, but historical data is preserved."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Products are soft-deleted, never permanently removed
 *   <li>Historical movements and balances remain accessible for reports
 *   <li>Only the product owner can delete their products
 * </ul>
 */
public interface DeleteProductUseCase {

  /**
   * Marks the specified product as deleted (soft delete).
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Command cannot be null
   *   <li>Command fields validation (userId, productId)
   *   <li>Product must exist for the specified user
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>UPDATE: Sets deleted_at timestamp on product record
   * </ul>
   *
   * @param command contains userId and productId to delete
   * @throws BusinessException if product not found or user doesn't own the product
   */
  void execute(DeleteProductCommand command) throws BusinessException;
}
