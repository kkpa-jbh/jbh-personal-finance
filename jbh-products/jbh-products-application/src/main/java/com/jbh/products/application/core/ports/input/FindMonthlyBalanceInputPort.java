package com.jbh.products.application.core.ports.input;

import static com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType.INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductPK;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FindMonthlyBalanceInputPort implements FindMonthlyBalanceUseCase {

  private final MonthlyBalanceService monthlyBalanceService;
  private final ProductsService accountService;

  public FindMonthlyBalanceInputPort(
      final MonthlyBalanceService monthlyBalanceService, final ProductsService accountService) {
    this.monthlyBalanceService = monthlyBalanceService;
    this.accountService = accountService;
  }

  @Override
  public List<MonthlyBalanceDTO> findByAccountAndPeriods(
      final ProductPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod)
      throws BusinessException {

    validatePeriodRange(startPeriod, endPeriod);
    accountService.findByUserAndProductId(accountPK.userId(), accountPK.accountId());

    return monthlyBalanceService.findByAccountAndPeriods(accountPK, startPeriod, endPeriod);
  }

  @Override
  public Map<ProductId, List<MonthlyBalanceDTO>> findByActiveProductsAndPeriods(
      final UUID userId, final YearMonth startPeriod, final YearMonth endPeriod)
      throws BusinessException {

    validatePeriodRange(startPeriod, endPeriod);

    final List<ProductDTO> activeProducts = accountService.findActiveByUserId(userId);
    if (activeProducts.isEmpty()) {
      return Collections.emptyMap();
    }

    final List<ProductId> productIds = activeProducts.stream().map(ProductDTO::id).toList();

    return monthlyBalanceService.findByProductIdsAndPeriods(productIds, startPeriod, endPeriod);
  }

  private void validatePeriodRange(final YearMonth startPeriod, final YearMonth endPeriod)
      throws BusinessException {
    if (startPeriod == null || endPeriod == null) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    if (startPeriod.isAfter(endPeriod)) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    if (endPeriod.isAfter(YearMonth.now())) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }
  }
}
