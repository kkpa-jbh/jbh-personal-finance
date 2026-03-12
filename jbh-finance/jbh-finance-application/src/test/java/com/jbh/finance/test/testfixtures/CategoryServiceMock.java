package com.jbh.finance.test.testfixtures;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.services.CategoryService;

public class CategoryServiceMock implements CategoryService {

  @Override
  public CategoryDTO findIncomeInitialBalance() {
    return (CategoryFixturesTestApp.INCOME_INITIAL_BALANCE);
  }

  @Override
  public CategoryDTO findIncomeTransfer() {
    return CategoryFixturesTestApp.INCOME_TRANSFER;
  }

  @Override
  public CategoryDTO findIncomeDividends() {
    return CategoryFixturesTestApp.INCOME_DIVIDENDS;
  }

  @Override
  public CategoryDTO findInvestmentToCloseIt() {
    return CategoryFixturesTestApp.INVESTMENT_TO_CLOSE_IT;
  }

  @Override
  public CategoryDTO findExpenseTransfer() {
    return CategoryFixturesTestApp.EXPENSE_TRANSFER;
  }

  @Override
  public CategoryDTO findExpenseRetefuente() {
    return CategoryFixturesTestApp.EXPENSE_RETEFUENTE;
  }

  @Override
  public CategoryDTO findExpenseUnknown() {
    return CategoryFixturesTestApp.EXPENSE_UNKNOWN;
  }

  @Override
  public CategoryDTO findIncomeOther() {
    return CategoryFixturesTestApp.INCOME_OTHER;
  }
}
