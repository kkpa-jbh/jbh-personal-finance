package com.jbh.products.application.core.usecases;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.balancehistory.BalanceHistoryResponseDTO;
import com.jbh.products.domain.vo.ProductPK;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface FindMonthlyBalanceUseCase {

  List<MonthlyBalanceDTO> findByAccountAndPeriods(
      ProductPK accountPK, YearMonth startPeriod, YearMonth endPeriod) throws BusinessException;

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
