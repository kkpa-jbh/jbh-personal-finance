package com.jbh.finance.infra.adapters.in.rest.product.request;

import com.jbh.finance.domain.product.vo.ProductMetadataKey;
import com.jbh.finance.domain.product.vo.ProductType;
import java.util.Map;

public record CreateProductRequest(
    String name, ProductType type, Map<ProductMetadataKey, Object> metadata) {}
