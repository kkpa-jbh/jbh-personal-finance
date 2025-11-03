package com.jbh.account.domain.validation.account.metrics;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.ProductType;

public final class AccountMetricsCalculatorFactory {

  private AccountMetricsCalculatorFactory() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static AccountMetricsCalculator getCalculator(final ProductType productType)
      throws AccountBusinessException {
    return switch (productType) {
      case SAVINGS -> new SavingsAccountMetricsCalculator();
      case CREDIT_CARD -> new CreditCardAccountMetricsCalculator();
      case INVESTMENT -> new InvestmentAccountMetricsCalculator();
      case CDT -> new CdtAccountMetricsCalculator();
      default ->
          throw new AccountBusinessException(
              BusinessDomainExceptionType.METRICS_CALCULATOR_NOT_IMPLEMENTED);
    };
  }
}
