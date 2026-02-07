package com.jbh.products.domain.product.service.metrics;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.domain.movement.MovementDomain;
import com.jbh.products.domain.product.ProductDomain;
import com.jbh.products.domain.product.vo.ProductMetadata;
import java.math.BigDecimal;

public interface ProductMetricsCalculator {

  BigDecimal calculateProfitBalance(ProductDomain accountDomain);

  BigDecimal calculateNetGrowthReate(
      BigDecimal openingBalance, ProductDomain accountDomain, BigDecimal movementAmount)
      throws BusinessException;

  ProductMetadata updateMetadata(ProductDomain productDomain, MovementDomain movement);
}
