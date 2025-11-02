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
    if (movementType == null || categoryName == null) {
      throw new IllegalArgumentException("MovementType and categoryName cannot be null");
    }

    final CategoryType categoryType;
    if (movementType.isDeposit()) {
      categoryType = IncomeCategory.findByName(categoryName);
    } else if (movementType.isWithdrawal()) {
      categoryType = ExpenseCategory.findByName(categoryName);
    } else {
      throw new IllegalArgumentException("Unsupported MovementType: " + movementType);
    }

    return new MovementCategoryDTO(categoryType);
  }

  @Override
  public String toString() {
    return "[Source: " + getSource() + ", Type: " + getType() + "]";
  }
}
