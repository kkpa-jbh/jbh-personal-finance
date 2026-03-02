package com.jbh.finance.domain.category.vo;

@SuppressWarnings({"PMD.LongVariable"})
public enum SystemCategoryAlias {
  // INCOME
  INCOME_OTHER("INC_OTHER"),
  INCOME_TRANSFER("INC_TRANSF"),
  INCOME_DIVIDENDS("INC_DVDS"),
  INCOME_INITIAL_BALANCE("INC_INIT_BALANCE"),
  // EXPENSE
  EXPENSE_TRANSFER("EXP_TRANSF"),
  EXPENSE_RETEFUENTE("EXP_RTFTE"),
  EXPENSE_INVESTMENT_WITHDRAWAL_TO_CLOSE_IT("INV_TO_CLOSE_IT"),
  EXPENSE_UNKNOWN("UNK");

  private final String alias;

  SystemCategoryAlias(final String alias) {
    this.alias = alias;
  }

  public String getAlias() {
    return alias;
  }
}
