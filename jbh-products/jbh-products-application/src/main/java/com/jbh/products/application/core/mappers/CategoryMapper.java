package com.jbh.products.application.core.mappers;

import com.jbh.products.domain.entity.MovementCategoryDomain;
import com.jbh.products.domain.vo.MovementCategoryDTO;

public final class CategoryMapper {

  private CategoryMapper() {}

  public static MovementCategoryDTO toDTO(final MovementCategoryDomain domain) {
    if (domain == null) {
      return null;
    }
    return MovementCategoryDTO.withType(domain.getType());
  }

  public static MovementCategoryDomain toDomain(final MovementCategoryDTO dto) {
    if (dto == null) {
      return null;
    }
    return MovementCategoryDomain.withDTO(dto);
  }
}
