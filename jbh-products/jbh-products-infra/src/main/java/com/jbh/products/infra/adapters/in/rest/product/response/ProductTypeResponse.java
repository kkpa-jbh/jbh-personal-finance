package com.jbh.products.infra.adapters.in.rest.product.response;

import com.jbh.products.infra.adapters.in.rest.product.response.ProductTypeResponse;

public record ProductTypeResponse(
    String name,
    String translationKey
) {
  public static ProductTypeResponse fromDTO(final ProductTypeDTO dto) {
    return new ProductTypeResponse(dto.name(), dto.translationKey());
  }
}
