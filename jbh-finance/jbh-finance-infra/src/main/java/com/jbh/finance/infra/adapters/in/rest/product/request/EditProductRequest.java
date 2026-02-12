package com.jbh.finance.infra.adapters.in.rest.product.request;

import com.jbh.finance.domain.product.vo.ProductMetadataKey;
import java.util.Map;

public record EditProductRequest(String name, Map<ProductMetadataKey, Object> metadata) {}
