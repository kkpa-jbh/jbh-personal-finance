package com.jbh.finance.application.feature.category.services;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;

/** Singleton class that cache categories */
public interface CategoryService {

  CategoryDTO findIncomeInitialBalance();

  CategoryDTO findIncomeTransfer();

  CategoryDTO findIncomeDividends();

  // Expenses

  CategoryDTO findInvestmentToCloseIt();

  CategoryDTO findExpenseTransfer();

  CategoryDTO findExpenseRetefuente();

  CategoryDTO findExpenseUnknown();

  CategoryDTO findIncomeOther();
}
