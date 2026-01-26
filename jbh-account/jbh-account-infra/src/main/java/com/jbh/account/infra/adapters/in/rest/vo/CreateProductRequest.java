package com.jbh.account.infra.adapters.in.rest.vo;

import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.account.domain.vo.ProductType;
import java.util.Map;

public record CreateProductRequest(
    String name, ProductType type, Map<ProductMetadataKey, Object> metadata) {}
