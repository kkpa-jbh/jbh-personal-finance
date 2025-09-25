package com.jbh.account.domain.vo;

import com.jbh.account.domain.entity.MovementCategoryDomain;

public class MovementCategoryDTO extends MovementCategoryDomain {

  public MovementCategoryDTO(final CategoryType categoryType) {
    super(categoryType);
  }

  public static MovementCategoryDTO withType(final CategoryType categoryType) {
    return new MovementCategoryDTO(categoryType);
  }

  @Override
  public String toString() {
    return "[Source: " + getSource() + ", Type: " + getType() + "]";
  }
}
