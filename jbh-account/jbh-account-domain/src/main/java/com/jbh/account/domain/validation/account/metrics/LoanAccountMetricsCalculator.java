package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;
import java.math.BigDecimal;

public class LoanAccountMetricsCalculator extends BaseAccountMetricsCalculator
    implements AccountMetricsCalculator {

  @Override
  public BigDecimal calculateProfitBalance(final ProductDomain accountDomain) {
    return JBH_ZERO;
  }

  @Override
  public BigDecimal calculateNetGrowthReate(
      final BigDecimal openingBalance,
      final ProductDomain accountDomain,
      final BigDecimal movementAmount)
      throws AccountBusinessException {
    return JBH_ZERO;
  }

  @Override
  @SuppressWarnings("PMD.LawOfDemeter")
  public ProductMetadata updateMetadata(
      final ProductDomain productDomain, final AccountMovementDomain movement) {
    final ProductMetadata loanMetadata = productDomain.getMetadata();
    BigDecimal totalAmountPaid = loanMetadata.getLoan().getTotalAmountPaid();

    final BigDecimal loanAmountPaid = movement.getMovementAmount();
    if (loanAmountPaid != null) {
      totalAmountPaid = totalAmountPaid.add(loanAmountPaid);
      totalAmountPaid = withJBHDecimals(totalAmountPaid);
      loanMetadata.getLoan().putTotalAmountPaid(totalAmountPaid);
    }
    return loanMetadata;
  }
}
