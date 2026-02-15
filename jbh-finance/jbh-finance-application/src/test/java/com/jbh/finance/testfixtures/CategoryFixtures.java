package com.jbh.finance.testfixtures;

import com.jbh.finance.domain.movement.CategoryDomain;
import com.jbh.finance.domain.movement.vo.ExpenseCategory;
import com.jbh.finance.domain.movement.vo.IncomeCategory;

/**
 * Test fixtures for CategoryDomain objects.
 * Provides pre-configured category instances for testing purposes.
 * <p>
 * This class centralizes test data creation to avoid polluting production code
 * with test-only constants.
 */
public final class CategoryFixtures {

  public static final CategoryDomain OTHER_INCOME =
      CategoryDomain.withCategoryType(IncomeCategory.OTHER);

  public static final CategoryDomain PERSONAL_EXPENSE =
      CategoryDomain.withCategoryType(ExpenseCategory.PERSONAL);

  private CategoryFixtures() {
    throw new AssertionError("Utility class - do not instantiate");
  }
}
