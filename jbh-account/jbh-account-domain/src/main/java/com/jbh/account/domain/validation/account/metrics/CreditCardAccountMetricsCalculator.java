package com.jbh.account.domain.validation.account.metrics;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;

public class CreditCardAccountMetricsCalculator extends BaseAccountMetricsCalculator
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
    final BigDecimal closingBalance = accountDomain.getCurrentBalance();
    return defaultNetGrowthRateCalculation(openingBalance, closingBalance, movementAmount);
  }

  @Override
  public ProductMetadata updateMetadata(
      final ProductDomain productDomain, final ProductMovementDomain movement) {
    return productDomain.getMetadata();
  }
}
