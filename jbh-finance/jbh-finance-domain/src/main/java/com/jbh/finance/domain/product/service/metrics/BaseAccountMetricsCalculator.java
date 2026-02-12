package com.jbh.finance.domain.product.service.metrics;

import com.jbh.finance.domain.monthlybalance.service.MoneyGrowthCalculator;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;

public class BaseAccountMetricsCalculator {
  protected final MoneyGrowthCalculator moneyGrowthCalculator = new MoneyGrowthCalculator();

  public BigDecimal defaultProfitBalanceCalculation(final ProductDomain accountDomain) {
    final BigDecimal currentBalance = accountDomain.getCurrentBalance();
    final BigDecimal movementBalance = accountDomain.getMovementBalance();
    return currentBalance.subtract(movementBalance);
  }

  public BigDecimal defaultNetGrowthRateCalculation(
      final BigDecimal openingBalance,
      final BigDecimal closingBalance,
      final BigDecimal movementAmount)
      throws BusinessException {
    return moneyGrowthCalculator.calculateGrowth(openingBalance, closingBalance, movementAmount);
  }
}
