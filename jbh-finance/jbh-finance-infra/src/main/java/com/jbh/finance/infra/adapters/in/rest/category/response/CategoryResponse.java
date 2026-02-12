package com.jbh.finance.infra.adapters.in.rest.category.response;

import com.jbh.finance.domain.movement.vo.CategorySource;
import com.jbh.finance.domain.movement.vo.CategoryType;

public record CategoryResponse(String name, String translationKey, CategorySource source) {
  public static CategoryResponse fromDTO(final CategoryType dto) {
    return new CategoryResponse(dto.getTypeName(), dto.getTranslationsKey(), dto.getSource());
  }
}
