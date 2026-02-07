package com.jbh.products.domain.product.vo.metadata;

import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.products.domain.product.vo.ProductMetadataKey;
import java.math.BigDecimal;

public abstract class BaseMetadata {

  protected BigDecimal getDecimal(final ProductMetadataKey key) {
    final Object result = get(key);
    return JbhMoneyUtils.toJBHDecimal(result);
  }

  protected abstract Object get(ProductMetadataKey key);
}
