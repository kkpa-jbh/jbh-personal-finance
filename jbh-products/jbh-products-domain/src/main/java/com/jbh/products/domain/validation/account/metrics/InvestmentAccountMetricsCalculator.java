package com.jbh.products.domain.validation.account.metrics;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.isNegativeOrZero;

import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.entity.ProductMovementDomain;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.commons.exception.BusinessException;
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
      throws BusinessException {
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
