package com.jbh.account.domain.entity;

import com.jbh.account.domain.vo.CategorySource;
import com.jbh.account.domain.vo.CategoryType;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;

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

  public CategorySource getSource() {
    return categoryType.getSource();
  }
}
