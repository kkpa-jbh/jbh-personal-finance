package com.jbh.finance.application.feature.movement.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.UUID;

/**
 * Deletes a financial movement from a product and reverses its effect on product balances.
 *
 * <p><strong>User Explanation:</strong> "Remove a mistakenly recorded transaction. The system will
 * undo the balance changes caused by the movement automatically."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Only movements created in the current month can be deleted
 *   <li>The movement must belong to a product owned by the requesting user
 *   <li>Deleting a movement reverses its effect on the product's current balance
 * </ul>
 */
public interface DeleteMovementUseCase {

  /**
   * Deletes a movement and reverses its effect on the product balance.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>User ID cannot be null
   *   <li>Product must exist and belong to the user
   *   <li>Movement must exist and belong to the product
   *   <li>Movement must have been created in the current month
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>DELETE: Movement record
   *   <li>UPDATE: Product current balance (reversed)
   * </ul>
   *
   * @param userId the user who owns the product
   * @param productId the product the movement belongs to
   * @param movementId the ID of the movement to delete
   * @throws BusinessException if the movement is not found, doesn't belong to the product, or
   *     cannot be removed per business rules
   */
  void deleteMovement(UUID userId, ProductId productId, UUID movementId) throws BusinessException;
}
