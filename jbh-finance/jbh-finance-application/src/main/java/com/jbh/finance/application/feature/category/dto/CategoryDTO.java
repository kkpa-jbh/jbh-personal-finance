package com.jbh.finance.application.feature.category.dto;

import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.INCOME_INITIAL_BALANCE;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT;

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

  private CategoryDTO(
      final CategoryTypeVO categoryType,
      final Long id,
      final Map<String, String> displayName,
      final boolean active) {
    this.categoryType = categoryType;
    this.categoryId = id;
    this.displayName = displayName;
    this.active = active;
  }

  public static CategoryDTO withInternalPurpose(final CategoryTypeVO categoryType, final Long id) {
    return new CategoryDTO(categoryType, id, null, true);
  }

  public static CategoryDTO withEntity(
      final CategoryTypeVO categoryType,
      final Long id,
      final Map<String, String> displayName,
      final boolean active) {
    return new CategoryDTO(categoryType, id, displayName, active);
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
    return is(INVESTMENT_WITHDRAWAL_TO_CLOSE_IT);
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
    return isNot(INVESTMENT_WITHDRAWAL_TO_CLOSE_IT);
  }
}
