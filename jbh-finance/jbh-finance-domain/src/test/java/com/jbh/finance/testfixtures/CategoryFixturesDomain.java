package com.jbh.finance.testfixtures;

import com.jbh.finance.domain.category.CategoryDomain;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.domain.category.vo.SystemCategoryAlias;

/**
 * Test fixtures for CategoryDomain objects. Provides pre-configured category instances for testing
 * purposes.
 *
 * <p>This class centralizes test data creation to avoid polluting production code with test-only
 * constants.
 */
public final class CategoryFixturesDomain {

  public static final CategoryDomain OTHER_INCOME =
      CategoryDomain.withCategoryType(new CategoryTypeVO(CategorySourceVO.INCOME, "OTHER"));

  public static final String SOCIAL_SECURITY = "SOCIAL_SECURITY";
  public static final CategoryDomain SOCIAL_SECURITY_EXPENSE =
      CategoryDomain.withCategoryType(
          new CategoryTypeVO(CategorySourceVO.EXPENSE, SOCIAL_SECURITY));
  public static final CategoryDomain UNKNOWN_EXPENSE =
      CategoryDomain.withCategoryType(
          new CategoryTypeVO(
              CategorySourceVO.EXPENSE, SystemCategoryAlias.EXPENSE_UNKNOWN.getAlias()));

  private CategoryFixturesDomain() {
    throw new AssertionError("Utility class - do not instantiate");
  }
}
