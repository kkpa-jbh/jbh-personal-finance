package com.jbh.products.domain.movement.vo;

import com.jbh.products.domain.movement.MovementCategoryDomain;

public class MovementCategoryVO extends MovementCategoryDomain {

  public MovementCategoryVO(final CategoryType categoryType) {
    super(categoryType);
  }

  public static MovementCategoryVO withType(final CategoryType categoryType) {
    return new MovementCategoryVO(categoryType);
  }

  public static MovementCategoryVO withName(
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

    return new MovementCategoryVO(categoryType);
  }

  @Override
  public String toString() {
    return "[Source: " + getSource() + ", Type: " + getType() + "]";
  }
}
