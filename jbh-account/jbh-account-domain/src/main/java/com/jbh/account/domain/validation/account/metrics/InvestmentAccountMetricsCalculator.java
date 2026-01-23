package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.isNegativeOrZero;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;
import java.math.BigDecimal;

public class InvestmentAccountMetricsCalculator extends BaseAccountMetricsCalculator
    implements ProductMetricsCalculator {

  @Override
  public BigDecimal calculateProfitBalance(final ProductDomain accountDomain) {
    return defaultProfitBalanceCalculation(accountDomain);
  }

  @Override
  public BigDecimal calculateNetGrowthReate(
      final BigDecimal openingBalance,
      final ProductDomain accountDomain,
      final BigDecimal movementAmount)
      throws ProductBusinessException {
    // When it's fully withdrawal
    if (accountDomain.isFullyWithdrawn() && isNegativeOrZero(accountDomain.getCurrentBalance())) {
      return moneyGrowthCalculator.calculateGrowth(openingBalance, movementAmount.abs(), JBH_ZERO);
    }
    return moneyGrowthCalculator.calculateGrowth(
        openingBalance, accountDomain.getCurrentBalance(), JBH_ZERO);
  }

  @Override
  public ProductMetadata updateMetadata(
      final ProductDomain productDomain, final ProductMovementDomain movement) {
    return productDomain.getMetadata();
  }
}
