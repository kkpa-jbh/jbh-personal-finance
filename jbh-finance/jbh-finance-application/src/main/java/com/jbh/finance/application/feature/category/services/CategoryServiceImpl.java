package com.jbh.finance.application.feature.category.services;

import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.EXPENSE_INVESTMENT_WITHDRAWAL_TO_CLOSE_IT;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.EXPENSE_RETEFUENTE;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.EXPENSE_TRANSFER;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.EXPENSE_UNKNOWN;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.INCOME_DIVIDENDS;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.INCOME_INITIAL_BALANCE;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.INCOME_OTHER;
import static com.jbh.finance.domain.category.vo.SystemCategoryAlias.INCOME_TRANSFER;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryServiceImpl implements CategoryService {

  private static final Logger LOG = LoggerFactory.getLogger(CategoryServiceImpl.class);
  private final CategoryQueryRepo categoryQueryRepo;
  private Map<String, CategoryDTO> categoriesMa;

  public CategoryServiceImpl(final CategoryQueryRepo categoryQueryRepo) {
    this.categoryQueryRepo = categoryQueryRepo;
    createMap();
  }

  private void createMap() {
    LOG.info("Creating categories MAP ");

    categoriesMa =
        categoryQueryRepo.findAllSystemCategories().stream()
            .collect(Collectors.toMap(CategoryDTO::getAlias, category -> category));
  }

  @Override
  public CategoryDTO findIncomeInitialBalance() {
    return categoriesMa.get(INCOME_INITIAL_BALANCE.getAlias());
  }

  @Override
  public CategoryDTO findIncomeTransfer() {
    return categoriesMa.get(INCOME_TRANSFER.getAlias());
  }

  @Override
  public CategoryDTO findIncomeDividends() {
    return categoriesMa.get(INCOME_DIVIDENDS.getAlias());
  }

  @Override
  public CategoryDTO findInvestmentToCloseIt() {
    return categoriesMa.get(EXPENSE_INVESTMENT_WITHDRAWAL_TO_CLOSE_IT.getAlias());
  }

  @Override
  public CategoryDTO findExpenseTransfer() {
    return categoriesMa.get(EXPENSE_TRANSFER.getAlias());
  }

  @Override
  public CategoryDTO findExpenseRetefuente() {
    return categoriesMa.get(EXPENSE_RETEFUENTE.getAlias());
  }

  @Override
  public CategoryDTO findExpenseUnknown() {
    return categoriesMa.get(EXPENSE_UNKNOWN.getAlias());
  }

  @Override
  public CategoryDTO findIncomeOther() {
    return categoriesMa.get(INCOME_OTHER.getAlias());
  }
}
