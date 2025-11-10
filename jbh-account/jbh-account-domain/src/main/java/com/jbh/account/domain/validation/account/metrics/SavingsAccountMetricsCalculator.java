package com.jbh.account.domain.validation.account.metrics;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public class SavingsAccountMetricsCalculator extends BaseAccountMetricsCalculator
    implements AccountMetricsCalculator {

  @Override
  public BigDecimal calculateProfitBalance(final ProductDomain accountDomain) {
    return defaultProfitBalanceCalculation(accountDomain);
  }

  @Override
  public BigDecimal calculateNetGrowthReate(
      final BigDecimal openingBalance,
      final ProductDomain accountDomain,
      final BigDecimal movementAmount)
      throws AccountBusinessException {
    final BigDecimal closingBalance = accountDomain.getCurrentBalance();
    return defaultNetGrowthRateCalculation(openingBalance, closingBalance, movementAmount);
  }
}
