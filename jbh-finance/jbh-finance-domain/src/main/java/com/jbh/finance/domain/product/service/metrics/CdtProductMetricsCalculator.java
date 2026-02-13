package com.jbh.finance.domain.product.service.metrics;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.isNegativeOrZero;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import java.math.BigDecimal;

public class CdtProductMetricsCalculator extends BaseProductMetricsCalculator
    implements ProductMetricsCalculator {

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
      throws BusinessException {
    final BigDecimal closingBalance = accountDomain.getCurrentBalance();
    if (isNegativeOrZero(closingBalance)) {
      return moneyGrowthCalculator.calculateGrowth(openingBalance, movementAmount.abs(), JBH_ZERO);
    }
    return JBH_ZERO;
  }

  @Override
  public ProductMetadata updateMetadata(
      final ProductDomain productDomain, final MovementDomain movement) {
    return productDomain.getMetadata();
  }
}
