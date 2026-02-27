package com.jbh.finance.domain.category;

import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.domain.category.vo.SystemCategoryAlias;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class CategoryDomain {
  private final CategoryTypeVO categoryType;

  protected CategoryDomain(final CategoryTypeVO categoryType) {
    this.categoryType = categoryType;
  }

  public static boolean isEmpty(final CategoryDomain category) {
    return category == null || category.getType() == null;
  }

  public CategoryTypeVO getType() {
    return categoryType;
  }

  public static CategoryDomain withCategoryType(final CategoryTypeVO categoryType) {
    return new CategoryDomain(categoryType);
  }

  public boolean isExpense() {
    return getSource() == CategorySourceVO.EXPENSE;
  }

  public CategorySourceVO getSource() {
    return categoryType.source();
  }

  public boolean isIncome() {
    return getSource() == CategorySourceVO.INCOME;
  }

  public boolean isInvestmentWithdrawalToCloseIt() {
    return getAlias()
        .equalsIgnoreCase(SystemCategoryAlias.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT.getAlias());
  }

  public String getAlias() {
    return categoryType.getAlias();
  }
}
