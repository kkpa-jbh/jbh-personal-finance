package com.jbh.finance.application.feature.monthlybalance.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistoryDTO;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Retrieves monthly balance history and summaries for products or users within specified date
 * ranges.
 *
 * <p><strong>User Explanation:</strong> "View your monthly balance history and track how your
 * wealth has grown over time. See summaries with total balances, growth rates, and
 * period-over-period changes."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Date ranges must be valid (start before end, not in future)
 *   <li>Calculates weighted average growth rate based on closing balances
 *   <li>Computes period change and percentage change between first and last periods
 *   <li>Current month may be excluded from end period if specified
 *   <li>Read-only operations with no database modifications
 * </ul>
 */
public interface FindMonthlyBalanceUseCase {

  /**
   * Finds raw monthly balance records for a product within a date range.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Start period and end period cannot be null
   *   <li>Start period must be before or equal to end period
   *   <li>End period cannot be in the future
   *   <li>Product must exist for the user
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: Monthly balance records for product within period range
   * </ul>
   *
   * @param productPK the product primary key (userId and productId)
   * @param startPeriod the start period (inclusive)
   * @param endPeriod the end period (inclusive)
   * @return list of monthly balance DTOs (may be empty)
   * @throws BusinessException if validation fails or product not found
   */
  List<MonthlyBalanceDTO> findMonthlyBalancesByProduct(
      ProductPK productPK, YearMonth startPeriod, YearMonth endPeriod) throws BusinessException;

  /**
   * Finds monthly balance history with calculated summary for a single product.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Start period and end period cannot be null
   *   <li>Start period must be before or equal to end period
   *   <li>End period cannot be in the future
   *   <li>Product must exist for the user
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: Monthly balance records for product within period range
   * </ul>
   *
   * @param productPK the product primary key (userId and productId)
   * @param startPeriod the start period (inclusive)
   * @param endPeriod the end period (may be exclusive if equals today)
   * @param today current year month for period range calculation
   * @return balance history response with summary and monthly data
   * @throws BusinessException if validation fails or product not found
   */
  BalanceHistoryDTO findBalanceHistoryByProduct(
      ProductPK productPK, YearMonth startPeriod, YearMonth endPeriod, YearMonth today)
      throws BusinessException;

  /**
   * Finds aggregated monthly balance history for all active products belonging to a user.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>Start period and end period cannot be null
   *   <li>Start period must be before or equal to end period
   *   <li>End period cannot be in the future
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: All active products for user
   *   <li>SELECT: Monthly balance records for all products within period range
   * </ul>
   *
   * @param userId the user ID
   * @param startPeriod the start period (inclusive)
   * @param endPeriod the end period (may be exclusive if equals today)
   * @param today current year month for period range calculation
   * @return balance history response with aggregated summary and monthly data across all products
   * @throws BusinessException if validation fails
   */
  BalanceHistoryDTO findBalanceHistoryByUser(
      UUID userId, YearMonth startPeriod, YearMonth endPeriod, YearMonth today)
      throws BusinessException;
}
