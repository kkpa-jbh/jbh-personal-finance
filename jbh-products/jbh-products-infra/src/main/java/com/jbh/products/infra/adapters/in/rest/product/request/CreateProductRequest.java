package com.jbh.products.infra.adapters.in.rest.product.request;

import com.jbh.products.domain.vo.ProductMetadataKey;
import com.jbh.products.domain.vo.ProductType;
import java.util.Map;

public record CreateProductRequest(
    String name, ProductType type, Map<ProductMetadataKey, Object> metadata) {}
