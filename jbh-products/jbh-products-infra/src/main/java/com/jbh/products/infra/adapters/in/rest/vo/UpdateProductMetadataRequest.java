package com.jbh.products.infra.adapters.in.rest.vo;

import com.jbh.products.domain.vo.ProductMetadataKey;
import java.util.Map;

public record UpdateProductMetadataRequest(Map<ProductMetadataKey, Object> metadata) {}
