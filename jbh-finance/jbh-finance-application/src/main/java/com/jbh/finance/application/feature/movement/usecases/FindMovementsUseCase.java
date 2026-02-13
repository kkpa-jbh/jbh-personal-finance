package com.jbh.finance.application.feature.movement.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.List;
import java.util.UUID;

/**
 * Retrieves financial movements for a specific product within a configurable time period.
 *
 * <p><strong>User Explanation:</strong> "View your recent transaction history for an productDTO.
 * You can see the last few months of deposits, withdrawals, and balance updates to track your
 * spending and income patterns."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Default time period is 3 months back (inclusive of current month)
 *   <li>Results are sorted by movement date in descending order (newest first)
 *   <li>Only movements belonging to the specified user and product are returned
 *   <li>Period calculation: if today is 2026-02 and monthsBack=3, returns movements from 2025-12
 *       to 2026-02
 * </ul>
 */
public interface FindMovementsUseCase {

  /**
   * Finds movements for a product within a specified number of months back from today.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>User ID cannot be null
   *   <li>Product ID cannot be null
   *   <li>Months back must be positive (defaults to 3 if not specified)
   *   <li>Product must exist and belong to the user
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: Movements filtered by user, product, and date range
   * </ul>
   *
   * @param userId the user who owns the product
   * @param productId the product ID to query movements from
   * @param monthsBack number of months to look back (inclusive), defaults to 3
   * @return list of movements sorted by date descending (newest first)
   * @throws BusinessException if product doesn't exist or doesn't belong to user
   */
  List<MovementDTO> findMovementsByProduct(UUID userId, ProductId productId, Integer monthsBack)
      throws BusinessException;
}
