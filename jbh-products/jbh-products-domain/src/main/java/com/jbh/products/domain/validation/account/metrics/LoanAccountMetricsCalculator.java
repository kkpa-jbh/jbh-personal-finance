package com.jbh.products.domain.validation.account.metrics;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;

import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.entity.ProductMovementDomain;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.commons.exception.BusinessException;
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
      final ProductDomain productDomain, final ProductMovementDomain movement) {
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
