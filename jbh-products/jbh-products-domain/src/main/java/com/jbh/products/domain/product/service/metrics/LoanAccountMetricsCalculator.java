package com.jbh.products.domain.product.service.metrics;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.domain.movement.MovementDomain;
import com.jbh.products.domain.product.ProductDomain;
import com.jbh.products.domain.product.vo.ProductMetadata;
import java.math.BigDecimal;

public class LoanAccountMetricsCalculator extends BaseAccountMetricsCalculator
    implements ProductMetricsCalculator {

  @Override
  public BigDecimal calculateProfitBalance(final ProductDomain accountDomain) {
    return JBH_ZERO;
  }

  @Override
  public BigDecimal calculateNetGrowthReate(
      final BigDecimal openingBalance,
      final ProductDomain accountDomain,
      final BigDecimal movementAmount)
      throws BusinessException {
    return JBH_ZERO;
  }

  @Override
  @SuppressWarnings("PMD.LawOfDemeter")
  public ProductMetadata updateMetadata(
      final ProductDomain productDomain, final MovementDomain movement) {
    final ProductMetadata loanMetadata = productDomain.getMetadata();
    BigDecimal totalAmountPaid = loanMetadata.findLoanMetadata().getTotalAmountPaid();

    final BigDecimal loanAmountPaid = movement.getMovementAmount();
    if (loanAmountPaid != null) {
      totalAmountPaid = totalAmountPaid.add(loanAmountPaid);
      totalAmountPaid = withJBHDecimals(totalAmountPaid);
      loanMetadata.findLoanMetadata().putTotalAmountPaid(totalAmountPaid);
    }
    return loanMetadata;
  }
}
