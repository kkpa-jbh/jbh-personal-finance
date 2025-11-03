package com.jbh.account.domain.validation.account.metrics;

import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public class CreditCardAccountMetricsCalculator extends BaseAccountMetricsCalculator
    implements AccountMetricsCalculator {

  @Override
  public BigDecimal calculateProfitBalance(final AccountDomain accountDomain) {
    return defaultProfitBalanceCalculation(accountDomain);
  }

  @Override
  public BigDecimal calculateNetGrowthReate(
      final BigDecimal openingBalance,
      final AccountDomain accountDomain,
      final BigDecimal movementAmount)
      throws AccountBusinessException {
    final BigDecimal closingBalance = accountDomain.getCurrentBalance();
    return defaultNetGrowthRateCalculation(openingBalance, closingBalance, movementAmount);
  }
}
