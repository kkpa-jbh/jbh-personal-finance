package com.jbh.products.domain.validation.account.metrics;

import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.entity.ProductMovementDomain;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;

public interface ProductMetricsCalculator {

  BigDecimal calculateProfitBalance(ProductDomain accountDomain);

  BigDecimal calculateNetGrowthReate(
      BigDecimal openingBalance, ProductDomain accountDomain, BigDecimal movementAmount)
      throws BusinessException;

  ProductMetadata updateMetadata(ProductDomain productDomain, ProductMovementDomain movement);
}
