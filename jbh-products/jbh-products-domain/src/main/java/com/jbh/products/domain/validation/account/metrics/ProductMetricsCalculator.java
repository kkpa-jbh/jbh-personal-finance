package com.jbh.products.domain.validation.account.metrics;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.domain.entity.MovementDomain;
import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.vo.ProductMetadata;
import java.math.BigDecimal;

public interface ProductMetricsCalculator {

  BigDecimal calculateProfitBalance(ProductDomain accountDomain);

  BigDecimal calculateNetGrowthReate(
      BigDecimal openingBalance, ProductDomain accountDomain, BigDecimal movementAmount)
      throws BusinessException;

  ProductMetadata updateMetadata(ProductDomain productDomain, MovementDomain movement);
}
