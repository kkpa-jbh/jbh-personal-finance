package com.jbh.account.domain.validation.account.metrics;

import com.jbh.account.domain.calculators.MoneyGrowthCalculator;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public class BaseAccountMetricsCalculator {
  protected final MoneyGrowthCalculator moneyGrowthCalculator = new MoneyGrowthCalculator();

  public BigDecimal defaultProfitBalanceCalculation(final AccountDomain accountDomain) {
    final BigDecimal currentBalance = accountDomain.getCurrentBalance();
    final BigDecimal movementBalance = accountDomain.getMovementBalance();
    return currentBalance.subtract(movementBalance);
  }

  public BigDecimal defaultNetGrowthRateCalculation(
      final BigDecimal openingBalance,
      final BigDecimal closingBalance,
      final BigDecimal movementAmount)
      throws AccountBusinessException {
    return moneyGrowthCalculator.calculateGrowth(openingBalance, closingBalance, movementAmount);
  }
}
