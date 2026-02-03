package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FindMonthlyBalanceUseCase {

  List<MonthlyBalanceDTO> findByAccountAndPeriods(
      ProductPK accountPK, YearMonth startPeriod, YearMonth endPeriod) throws BusinessException;

  /**
   * Finds monthly balances for all active products belonging to a user within a date range.
   *
   * @param userId the user ID
   * @param startPeriod the start period (inclusive)
   * @param endPeriod the end period (inclusive)
   * @return a map where the key is the ProductId and the value is the list of monthly balances
   */
  Map<ProductId, List<MonthlyBalanceDTO>> findByActiveProductsAndPeriods(
      UUID userId, YearMonth startPeriod, YearMonth endPeriod) throws BusinessException;
}
