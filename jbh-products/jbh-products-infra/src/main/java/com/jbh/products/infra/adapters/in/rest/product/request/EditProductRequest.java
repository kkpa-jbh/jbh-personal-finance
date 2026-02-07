package com.jbh.products.infra.adapters.in.rest.product.request;

import com.jbh.products.domain.product.vo.ProductMetadataKey;
import java.util.Map;

public record EditProductRequest(String name, Map<ProductMetadataKey, Object> metadata) {}
