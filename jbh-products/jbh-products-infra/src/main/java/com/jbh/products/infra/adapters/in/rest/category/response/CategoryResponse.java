package com.jbh.products.infra.adapters.in.rest.category.response;

import com.jbh.products.domain.vo.CategorySource;
import com.jbh.products.infra.adapters.in.rest.category.response.CategoryResponse;

public record CategoryResponse(
    String name,
    String translationKey,
    CategorySource source
) {
  public static CategoryResponse fromDTO(final CategoryDTO dto) {
    return new CategoryResponse(dto.name(), dto.translationKey(), dto.source());
  }
}
