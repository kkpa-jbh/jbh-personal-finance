package com.jbh.account.infra.adapters.in.rest.vo;

import com.jbh.account.domain.vo.ProductType;

public record CreateProductRequest(String name, ProductType type) {}
