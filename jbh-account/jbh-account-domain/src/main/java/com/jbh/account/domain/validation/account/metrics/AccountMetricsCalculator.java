package com.jbh.account.domain.validation.account.metrics;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public interface AccountMetricsCalculator {

  BigDecimal calculateProfitBalance(ProductDomain accountDomain);

  BigDecimal calculateNetGrowthReate(
      BigDecimal openingBalance, ProductDomain accountDomain, BigDecimal movementAmount)
      throws AccountBusinessException;
}
