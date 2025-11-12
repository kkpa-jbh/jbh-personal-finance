package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
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
  public ProductMetadata updateMetadata(
      final ProductDomain productDomain, final AccountMovementDomain movement) {
    final ProductMetadata loanMetadata =
        ProductMetadata.fromMap(productDomain.getMetadata().asMap());

    if (loanMetadata.hasKey(ProductMetadataKey.LOAN_TOTAL_AMOUNT_PAID)) {
      BigDecimal totalAmountPaid = loanMetadata.getLoanTotalAmountPaid();

      totalAmountPaid = totalAmountPaid.add(movement.getMovementAmount());
      totalAmountPaid = withJBHDecimals(totalAmountPaid);

      loanMetadata.putLoanTotalAmountPaid(totalAmountPaid);
    }

    return loanMetadata;
  }
}
