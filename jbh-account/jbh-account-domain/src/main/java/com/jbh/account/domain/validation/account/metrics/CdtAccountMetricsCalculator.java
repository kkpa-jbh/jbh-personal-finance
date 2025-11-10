package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.isNegativeOrZero;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public class CdtAccountMetricsCalculator extends BaseAccountMetricsCalculator
    implements AccountMetricsCalculator {

  @Override
  public BigDecimal calculateProfitBalance(final ProductDomain accountDomain) {
    if (accountDomain.isFullyWithdrawn()) {
      return accountDomain.getMovementBalance().abs();
    }
    return JBH_ZERO;
  }

  @Override
  public BigDecimal calculateNetGrowthReate(
      final BigDecimal openingBalance,
      final ProductDomain accountDomain,
      final BigDecimal movementAmount)
      throws AccountBusinessException {
    final BigDecimal closingBalance = accountDomain.getCurrentBalance();
    if (isNegativeOrZero(closingBalance)) {
      return moneyGrowthCalculator.calculateGrowth(openingBalance, movementAmount.abs(), JBH_ZERO);
    }
    return JBH_ZERO;
  }
}
