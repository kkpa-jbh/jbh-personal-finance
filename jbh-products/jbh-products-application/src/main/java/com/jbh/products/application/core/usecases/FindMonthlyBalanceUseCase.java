package com.jbh.products.application.core.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.balancehistory.BalanceHistoryResponseDTO;
import com.jbh.products.domain.vo.ProductPK;
import java.time.YearMonth;
import java.util.UUID;

public interface FindMonthlyBalanceUseCase {

  BalanceHistoryResponseDTO findHistoryByProduct(
      ProductPK productPK, YearMonth startPeriod, YearMonth endPeriod, YearMonth today)
      throws BusinessException;

  /**
   * Finds monthly balances for all active products belonging to a user within a date range.
   *
   * @param userId the user ID
   * @param startPeriod the start period
   * @param endPeriod the end period (exclusive if equals to today)
   * @param today current year month
   */
  BalanceHistoryResponseDTO findBalanceHistoryByUser(
      UUID userId, YearMonth startPeriod, YearMonth endPeriod, YearMonth today)
      throws BusinessException;
}
