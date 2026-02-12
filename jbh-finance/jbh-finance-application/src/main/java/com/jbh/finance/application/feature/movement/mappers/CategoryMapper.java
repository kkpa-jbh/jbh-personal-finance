package com.jbh.finance.application.feature.movement.mappers;

import com.jbh.finance.domain.movement.MovementCategoryDomain;
import com.jbh.finance.domain.movement.vo.MovementCategoryVO;

public final class CategoryMapper {

  private CategoryMapper() {}

  public static MovementCategoryVO toDTO(final MovementCategoryDomain domain) {
    if (domain == null) {
      return null;
    }
    return MovementCategoryVO.withType(domain.getType());
  }

  public static MovementCategoryDomain toDomain(final MovementCategoryVO dto) {
    if (dto == null) {
      return null;
    }
    return MovementCategoryDomain.withDTO(dto);
  }
}
