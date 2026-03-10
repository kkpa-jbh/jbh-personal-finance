package com.jbh.finance.application.feature.category.dto;

import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.EXPENSE_INVESTMENT_WITHDRAWAL_TO_CLOSE_IT;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.INCOME_INITIAL_BALANCE;

import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.domain.category.vo.SystemCategoryAlias;
import java.util.Map;
import lombok.Getter;

/** // Balance snapshot does not have category. */
@SuppressWarnings("PMD.CyclomaticComplexity")
@Getter
public final class CategoryDTO {

  private final CategoryTypeVO categoryType;
  private final Long categoryId;
  private final Map<String, String> displayName;
  private final boolean active;
  private final Map<String, String> description;

  private CategoryDTO(
      final CategoryTypeVO categoryType,
      final Long id,
      final Map<String, String> displayName,
      final boolean active,
      final Map<String, String> description) {
    this.categoryType = categoryType;
    this.categoryId = id;
    this.displayName = displayName;
    this.active = active;
    this.description = description;
  }

  public static CategoryDTO withInternalPurpose(final CategoryTypeVO categoryType, final Long id) {
    return new CategoryDTO(categoryType, id, null, true, null);
  }

  public static CategoryDTO withEntity(
      final CategoryTypeVO categoryType,
      final Long id,
      final Map<String, String> displayName,
      final boolean active,
      final Map<String, String> description) {
    return new CategoryDTO(categoryType, id, displayName, active, description);
  }

  @Override
  public String toString() {
    return "[Source: " + getSource() + ", Alias: " + getAlias() + "]";
  }

  public CategorySourceVO getSource() {
    return categoryType.getSource();
  }

  public String getAlias() {
    return categoryType.getAlias();
  }

  public boolean isInvestmentToCloseIt() {
    return is(EXPENSE_INVESTMENT_WITHDRAWAL_TO_CLOSE_IT);
  }

  private boolean is(final SystemCategoryAlias alias) {
    return this.getAlias().equalsIgnoreCase(alias.getAlias());
  }

  public boolean isNotIncomeTransfer() {
    return isNot(SystemCategoryAlias.INCOME_TRANSFER);
  }

  private boolean isNot(final SystemCategoryAlias systemCategoryAlias) {
    return !this.getAlias().equalsIgnoreCase(systemCategoryAlias.getAlias());
  }

  public boolean isNotIncomeInitialBalance() {
    return isNot(INCOME_INITIAL_BALANCE);
  }

  public boolean isNotInvestmentToCloseIt() {
    return isNot(EXPENSE_INVESTMENT_WITHDRAWAL_TO_CLOSE_IT);
  }
}
