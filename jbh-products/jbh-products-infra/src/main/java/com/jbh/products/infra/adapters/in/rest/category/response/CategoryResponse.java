package com.jbh.products.infra.adapters.in.rest.category.response;

import com.jbh.products.domain.movement.vo.CategorySource;
import com.jbh.products.domain.movement.vo.CategoryType;

public record CategoryResponse(String name, String translationKey, CategorySource source) {
  public static CategoryResponse fromDTO(final CategoryType dto) {
    return new CategoryResponse(dto.getTypeName(), dto.getTranslationsKey(), dto.getSource());
  }
}
