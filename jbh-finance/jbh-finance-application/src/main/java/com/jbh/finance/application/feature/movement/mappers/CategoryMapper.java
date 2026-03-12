package com.jbh.finance.application.feature.movement.mappers;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.domain.category.CategoryDomain;

public final class CategoryMapper {

  private CategoryMapper() {}

  public static CategoryDTO toDTO(final CategoryDomain domain) {
    if (domain == null) {
      return null;
    }
    return CategoryDTO.withInternalPurpose(domain.getType(), domain.getCategoryId());
  }

  public static CategoryDomain toDomain(final CategoryDTO dto) {
    if (dto == null) {
      return null;
    }
    return CategoryDomain.withCategoryType(dto.getCategoryType(), dto.getCategoryId());
  }
}
