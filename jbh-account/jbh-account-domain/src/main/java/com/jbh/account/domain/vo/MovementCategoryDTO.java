package com.jbh.account.domain.vo;

import com.jbh.account.domain.entity.MovementCategoryDomain;

public class MovementCategoryDTO extends MovementCategoryDomain {

  public MovementCategoryDTO(final CategoryType categoryType) {
    super(categoryType);
  }

  public static MovementCategoryDTO withType(final CategoryType categoryType) {
    return new MovementCategoryDTO(categoryType);
  }

  public static MovementCategoryDTO withName(
      final MovementType movementType, final String categoryName) {
    if (movementType == null) {
      throw new IllegalArgumentException("Movement type cannot be null");
    }
    if (categoryName == null) {
      throw new IllegalArgumentException("Category name cannot be null");
    }
    if (movementType == MovementType.DEPOSIT) {
      return new MovementCategoryDTO(IncomeCategory.findByName(categoryName));
    }
    return new MovementCategoryDTO(ExpenseCategory.findByName(categoryName));
  }

  @Override
  public String toString() {
    return "[Source: " + getSource() + ", Type: " + getType() + "]";
  }
}
