package com.jbh.finance.domain.product.service.metrics;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import java.math.BigDecimal;

public class SavingsAccountMetricsCalculator extends BaseAccountMetricsCalculator
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
      final ProductDomain productDomain, final MovementDomain movement) {
    return productDomain.getMetadata();
  }
}
