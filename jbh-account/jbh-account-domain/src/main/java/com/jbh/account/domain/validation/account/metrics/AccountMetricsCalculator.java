package com.jbh.account.domain.validation.account.metrics;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;
import java.math.BigDecimal;

public interface AccountMetricsCalculator {

  BigDecimal calculateProfitBalance(ProductDomain accountDomain);

  BigDecimal calculateNetGrowthReate(
      BigDecimal openingBalance, ProductDomain accountDomain, BigDecimal movementAmount)
      throws AccountBusinessException;

  ProductMetadata updateMetadata(ProductDomain productDomain, AccountMovementDomain movement);
}
