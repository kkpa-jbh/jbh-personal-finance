package com.jbh.finance.domain.movement.vo;

import com.jbh.finance.domain.movement.MovementCategoryDomain;

@SuppressWarnings("PMD.CyclomaticComplexity")
public class MovementCategoryVO extends MovementCategoryDomain {

  public MovementCategoryVO(final CategoryType categoryType) {
    super(categoryType);
  }

  public static MovementCategoryVO withType(final CategoryType categoryType) {
    return new MovementCategoryVO(categoryType);
  }

  // TODO Check why this method is used.
  public static MovementCategoryVO withName(
      final MovementType movementType, final String categoryName) {
    if (movementType == null && categoryName == null) {
      throw new IllegalArgumentException("MovementType and categoryName cannot be null");
    }

    if (movementType == null) {
      throw new IllegalArgumentException("MovementType cannot be null");
    }

    // Balance snapshot does not have category.
    if (movementType.isBalanceSnapshot()) {
      return null;
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
