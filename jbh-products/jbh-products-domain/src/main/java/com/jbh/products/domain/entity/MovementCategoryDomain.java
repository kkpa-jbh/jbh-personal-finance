package com.jbh.products.domain.entity;

import com.jbh.products.domain.vo.CategorySource;
import com.jbh.products.domain.vo.CategoryType;
import com.jbh.products.domain.vo.ExpenseCategory;
import com.jbh.products.domain.vo.IncomeCategory;
import com.jbh.products.domain.vo.MovementCategoryDTO;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class MovementCategoryDomain {
  public static final MovementCategoryDomain OTHER_INCOME_CATEGORY =
      withCategoryType(IncomeCategory.OTHER);

  public static final MovementCategoryDomain PERSONAL_EXPENSE_CATEGORY =
      withCategoryType(ExpenseCategory.PERSONAL);

  private final CategoryType categoryType;

  public MovementCategoryDomain(final CategoryType categoryType) {
    this.categoryType = categoryType;
  }

  public static boolean isEmpty(final MovementCategoryDomain category) {
    return category == null || category.getType() == null;
  }

  public CategoryType getType() {
    return categoryType;
  }

  public static MovementCategoryDomain withDTO(final MovementCategoryDTO movementCategoryDTO) {
    if (movementCategoryDTO == null) {
      return null;
    }
    return withCategoryType(movementCategoryDTO.getType());
  }

  public static MovementCategoryDomain withCategoryType(final CategoryType categoryType) {
    return new MovementCategoryDomain(categoryType);
  }

  public boolean isExpense() {
    return getSource() == CategorySource.EXPENSE;
  }

  public CategorySource getSource() {
    return categoryType.getSource();
  }

  public boolean isIncome() {
    return getSource() == CategorySource.INCOME;
  }
}
