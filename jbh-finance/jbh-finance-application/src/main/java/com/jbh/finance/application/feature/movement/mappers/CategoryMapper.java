package com.jbh.finance.application.feature.movement.mappers;

import com.jbh.finance.domain.movement.CategoryDomain;
import com.jbh.finance.domain.movement.vo.MovementCategoryVO;

public final class CategoryMapper {

  private CategoryMapper() {}

  public static MovementCategoryVO toDTO(final CategoryDomain domain) {
    if (domain == null) {
      return null;
    }
    return MovementCategoryVO.withType(domain.getType());
  }

  public static CategoryDomain toDomain(final MovementCategoryVO dto) {
    if (dto == null) {
      return null;
    }
    return CategoryDomain.withDTO(dto);
  }
}
