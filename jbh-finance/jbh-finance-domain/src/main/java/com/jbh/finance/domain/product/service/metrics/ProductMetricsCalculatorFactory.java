package com.jbh.finance.domain.product.service.metrics;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.domain.product.vo.ProductType;

public final class ProductMetricsCalculatorFactory {

  private ProductMetricsCalculatorFactory() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static ProductMetricsCalculator getCalculator(final ProductType productType) {
    return switch (productType) {
      case SAVINGS -> new SavingsAccountMetricsCalculator();
      case CREDIT_CARD -> new CreditCardAccountMetricsCalculator();
      case INVESTMENT -> new InvestmentAccountMetricsCalculator();
      case CDT -> new CdtAccountMetricsCalculator();
      case LOAN -> new LoanAccountMetricsCalculator();
      default ->
          throw new GenericSpecificationException(
              "Metrics calculator not implemented for product type");
    };
  }
}
