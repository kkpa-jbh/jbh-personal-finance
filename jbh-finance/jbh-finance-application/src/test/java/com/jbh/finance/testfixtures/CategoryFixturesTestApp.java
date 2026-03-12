package com.jbh.finance.testfixtures;

import static com.jbh.finance.domain.category.vo.CategorySourceVO.EXPENSE;
import static com.jbh.finance.domain.category.vo.CategorySourceVO.INCOME;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.domain.category.CategoryDomain;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.domain.category.vo.SystemCategoryAlias;

/**
 * Test fixtures for CategoryDomain and MovementCategoryVO objects. Provides pre-configured category
 * instances for testing purposes.
 *
 * <p>This class centralizes test data creation to avoid polluting production code with test-only
 * constants and reduce duplication across test files.
 *
 * <p>Two types of fixtures are provided:
 *
 * <ul>
 *   <li>CategoryDomain fixtures with descriptive suffixes (e.g., SALARY_INCOME, PERSONAL_EXPENSE)
 *   <li>MovementCategoryVO fixtures with simple names (e.g., SALARY, PERSONAL) - most commonly used
 * </ul>
 *
 * <p>Usage in tests:
 *
 * <pre>
 * import static com.jbh.finance.testfixtures.CategoryFixtures.SALARY;
 * import static com.jbh.finance.testfixtures.CategoryFixtures.PERSONAL;
 *
 * AddMovementCommand command = AddMovementCommandTestBuilder.withCategory(date, amount, SALARY);
 * </pre>
 */
public final class CategoryFixturesTestApp {

  public static final CategoryDomain UNKNOWN_EXPENSE =
      domain(CategorySourceVO.EXPENSE, SystemCategoryAlias.EXPENSE_UNKNOWN.getAlias());
  public static final CategoryDomain INCOME_DEPOSIT_DOMAIN = domain(INCOME, "DEPOSIT");
  // CategoryDomain fixtures - Expense categories
  public static final CategoryDomain SOCIAL_SECURITY_EXPENSE =
      domain(CategorySourceVO.EXPENSE, "SOCIAL_SECURITY");
  private static long categoryId = 1L;
  public static final CategoryDTO EXPENSE_TRANSFER =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(EXPENSE, SystemCategoryAlias.EXPENSE_TRANSFER.getAlias()),
          categoryId++);
  ;
  ;
  public static final CategoryDTO INCOME_DIVIDENDS =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(INCOME, SystemCategoryAlias.INCOME_DIVIDENDS.getAlias()),
          categoryId++);

  public static final CategoryDTO INCOME_OTHER =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(INCOME, SystemCategoryAlias.INCOME_OTHER.getAlias()), categoryId++);
  ;
  public static final CategoryDTO INCOME_TRANSFER =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(INCOME, SystemCategoryAlias.INCOME_TRANSFER.getAlias()), categoryId++);
  // MovementCategoryVO fixtures - Income categories
  public static final CategoryDTO SALARY =
      CategoryDTO.withInternalPurpose(new CategoryTypeVO(INCOME, "SALARY"), categoryId++);
  public static final CategoryDTO INVESTMENT_TO_CLOSE_IT =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(
              EXPENSE, SystemCategoryAlias.EXPENSE_INVESTMENT_WITHDRAWAL_TO_CLOSE_IT.getAlias()),
          categoryId++);
  ;
  public static final CategoryDTO INCOME_INITIAL_BALANCE =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(INCOME, SystemCategoryAlias.INCOME_INITIAL_BALANCE.getAlias()),
          categoryId++);

  public static final CategoryDTO INCOME_DEPOSIT =
      CategoryDTO.withInternalPurpose(new CategoryTypeVO(INCOME, "DEPOSIT"), categoryId++);
  public static final CategoryDTO OTHER_INCOME_MOVEMENT =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(INCOME, "OTHER_INCOME_MOVEMENT"), categoryId++);

  public static final CategoryDTO PUBLIC_SERVICES =
      CategoryDTO.withInternalPurpose(new CategoryTypeVO(EXPENSE, "PUBLIC_SERVICES"), categoryId++);

  public static final CategoryDTO PERSONAL =
      CategoryDTO.withInternalPurpose(new CategoryTypeVO(EXPENSE, "PERSONAL"), categoryId++);

  public static final CategoryDTO EXPENSE_RETEFUENTE =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(EXPENSE, SystemCategoryAlias.EXPENSE_RETEFUENTE.getAlias()),
          categoryId++);

  public static final CategoryDTO EXPENSE_UNKNOWN =
      CategoryDTO.withInternalPurpose(
          new CategoryTypeVO(EXPENSE, SystemCategoryAlias.EXPENSE_UNKNOWN.getAlias()),
          categoryId++);

  private CategoryFixturesTestApp() {
    throw new AssertionError("Utility class - do not instantiate");
  }

  private static CategoryDomain domain(final CategorySourceVO source, final String alias) {
    return CategoryDomain.withCategoryType(new CategoryTypeVO(source, alias), null);
  }
}
