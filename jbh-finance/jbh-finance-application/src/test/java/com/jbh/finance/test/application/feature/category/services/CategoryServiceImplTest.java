package com.jbh.finance.test.application.feature.category.services;

import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.EXPENSE_RETEFUENTE;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.EXPENSE_TRANSFER;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.EXPENSE_UNKNOWN;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.INCOME_DIVIDENDS;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.INCOME_INITIAL_BALANCE;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.INCOME_OTHER;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.INCOME_TRANSFER;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.INVESTMENT_TO_CLOSE_IT;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.ports.output.CategoryQueryRepo;
import com.jbh.finance.application.feature.category.services.CategoryServiceImpl;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CategoryServiceImplTest {

  @Mock private CategoryQueryRepo categoryQueryRepo;

  private CategoryServiceImpl categoryService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(categoryQueryRepo.findAllSystemCategories()).thenReturn(allSystemCategories());
    categoryService = new CategoryServiceImpl(categoryQueryRepo);
  }

  private List<CategoryDTO> allSystemCategories() {
    return List.of(
        INCOME_INITIAL_BALANCE,
        INCOME_TRANSFER,
        INCOME_DIVIDENDS,
        INCOME_OTHER,
        INVESTMENT_TO_CLOSE_IT,
        EXPENSE_TRANSFER,
        EXPENSE_RETEFUENTE,
        EXPENSE_UNKNOWN);
  }

  @Test
  void shouldReturnIncomeInitialBalance_WhenCategoryExists() {
    // When
    final CategoryDTO result = categoryService.findIncomeInitialBalance();

    // Then
    assertSame(INCOME_INITIAL_BALANCE, result);
  }

  @Test
  void shouldReturnIncomeTransfer_WhenCategoryExists() {
    // When
    final CategoryDTO result = categoryService.findIncomeTransfer();

    // Then
    assertSame(INCOME_TRANSFER, result);
  }

  @Test
  void shouldReturnIncomeDividends_WhenCategoryExists() {
    // When
    final CategoryDTO result = categoryService.findIncomeDividends();

    // Then
    assertSame(INCOME_DIVIDENDS, result);
  }

  @Test
  void shouldReturnInvestmentToCloseIt_WhenCategoryExists() {
    // When
    final CategoryDTO result = categoryService.findInvestmentToCloseIt();

    // Then
    assertSame(INVESTMENT_TO_CLOSE_IT, result);
  }

  @Test
  void shouldReturnExpenseTransfer_WhenCategoryExists() {
    // When
    final CategoryDTO result = categoryService.findExpenseTransfer();

    // Then
    assertSame(EXPENSE_TRANSFER, result);
  }

  @Test
  void shouldReturnExpenseRetefuente_WhenCategoryExists() {
    // When
    final CategoryDTO result = categoryService.findExpenseRetefuente();

    // Then
    assertSame(EXPENSE_RETEFUENTE, result);
  }

  @Test
  void shouldReturnExpenseUnknown_WhenCategoryExists() {
    // When
    final CategoryDTO result = categoryService.findExpenseUnknown();

    // Then
    assertSame(EXPENSE_UNKNOWN, result);
  }

  @Test
  void shouldReturnIncomeOther_WhenCategoryExists() {
    // When
    final CategoryDTO result = categoryService.findIncomeOther();

    // Then
    assertSame(INCOME_OTHER, result);
  }

  @Test
  void shouldReturnNull_WhenRepoProvidesEmptyList() {
    // Given
    when(categoryQueryRepo.findAllSystemCategories()).thenReturn(Collections.emptyList());
    final CategoryServiceImpl emptyService = new CategoryServiceImpl(categoryQueryRepo);

    // When & Then
    assertNull(emptyService.findIncomeInitialBalance());
    assertNull(emptyService.findIncomeTransfer());
    assertNull(emptyService.findIncomeDividends());
    assertNull(emptyService.findInvestmentToCloseIt());
    assertNull(emptyService.findExpenseTransfer());
    assertNull(emptyService.findExpenseRetefuente());
    assertNull(emptyService.findExpenseUnknown());
    assertNull(emptyService.findIncomeOther());
  }

  @Test
  void shouldReturnNull_WhenOnlyPartialCategoriesAreLoaded() {
    // Given - repo only returns income categories, no expense categories
    when(categoryQueryRepo.findAllSystemCategories())
        .thenReturn(
            List.of(INCOME_INITIAL_BALANCE, INCOME_TRANSFER, INCOME_DIVIDENDS, INCOME_OTHER));
    final CategoryServiceImpl partialService = new CategoryServiceImpl(categoryQueryRepo);

    // When & Then - income categories resolve correctly
    assertSame(INCOME_INITIAL_BALANCE, partialService.findIncomeInitialBalance());
    assertSame(INCOME_TRANSFER, partialService.findIncomeTransfer());
    assertSame(INCOME_DIVIDENDS, partialService.findIncomeDividends());
    assertSame(INCOME_OTHER, partialService.findIncomeOther());

    // Expense/investment categories are absent
    assertNull(partialService.findExpenseTransfer());
    assertNull(partialService.findExpenseRetefuente());
    assertNull(partialService.findExpenseUnknown());
    assertNull(partialService.findInvestmentToCloseIt());
  }
}
