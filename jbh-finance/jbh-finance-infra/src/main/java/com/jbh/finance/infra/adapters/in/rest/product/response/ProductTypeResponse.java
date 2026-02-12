package com.jbh.finance.infra.adapters.in.rest.product.response;

import com.jbh.finance.domain.product.vo.ProductType;

public record ProductTypeResponse(String name, String translationKey) {
  public static ProductTypeResponse fromDTO(final ProductType dto) {
    return new ProductTypeResponse(dto.name(), dto.getTranslationKey());
  }
}
