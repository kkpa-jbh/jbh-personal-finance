package com.jbh.finance.domain.movement;

import com.jbh.finance.domain.movement.vo.CategorySource;
import com.jbh.finance.domain.movement.vo.CategoryType;
import com.jbh.finance.domain.movement.vo.ExpenseCategory;
import com.jbh.finance.domain.movement.vo.IncomeCategory;
import com.jbh.finance.domain.movement.vo.MovementCategoryVO;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class CategoryDomain {
  public static final CategoryDomain OTHER_INCOME_CATEGORY = withCategoryType(IncomeCategory.OTHER);

  public static final CategoryDomain PERSONAL_EXPENSE_CATEGORY =
      withCategoryType(ExpenseCategory.PERSONAL);

  private final CategoryType categoryType;

  public CategoryDomain(final CategoryType categoryType) {
    this.categoryType = categoryType;
  }

  public static boolean isEmpty(final CategoryDomain category) {
    return category == null || category.getType() == null;
  }

  public CategoryType getType() {
    return categoryType;
  }

  public static CategoryDomain withDTO(final MovementCategoryVO movementCategoryDTO) {
    if (movementCategoryDTO == null) {
      return null;
    }
    return withCategoryType(movementCategoryDTO.getType());
  }

  public static CategoryDomain withCategoryType(final CategoryType categoryType) {
    return new CategoryDomain(categoryType);
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
