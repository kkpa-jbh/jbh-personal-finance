package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.isNegativeOrZero;

import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public class InvestmentAccountMetricsCalculator extends BaseAccountMetricsCalculator
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
    // When it's fully withdrawal
    if (accountDomain.isFullyWithdrawn() && isNegativeOrZero(accountDomain.getCurrentBalance())) {
      return moneyGrowthCalculator.calculateGrowth(openingBalance, movementAmount.abs(), JBH_ZERO);
    }
    return moneyGrowthCalculator.calculateGrowth(
        openingBalance, accountDomain.getCurrentBalance(), JBH_ZERO);
  }
}
