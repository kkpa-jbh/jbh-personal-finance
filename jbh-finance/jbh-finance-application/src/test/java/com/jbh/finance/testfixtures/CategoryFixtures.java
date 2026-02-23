package com.jbh.finance.testfixtures;

import com.jbh.finance.domain.movement.CategoryDomain;
import com.jbh.finance.domain.movement.vo.ExpenseCategory;
import com.jbh.finance.domain.movement.vo.IncomeCategory;
import com.jbh.finance.domain.movement.vo.MovementCategoryVO;

/**
 * Test fixtures for CategoryDomain and MovementCategoryVO objects.
 * Provides pre-configured category instances for testing purposes.
 * <p>
 * This class centralizes test data creation to avoid polluting production code
 * with test-only constants and reduce duplication across test files.
 * <p>
 * Two types of fixtures are provided:
 * <ul>
 *   <li>CategoryDomain fixtures with descriptive suffixes (e.g., SALARY_INCOME, PERSONAL_EXPENSE)</li>
 *   <li>MovementCategoryVO fixtures with simple names (e.g., SALARY, PERSONAL) - most commonly used</li>
 * </ul>
 * <p>
 * Usage in tests:
 * <pre>
 * import static com.jbh.finance.testfixtures.CategoryFixtures.SALARY;
 * import static com.jbh.finance.testfixtures.CategoryFixtures.PERSONAL;
 *
 * AddMovementCommand command = AddMovementCommandTestBuilder.withCategory(date, amount, SALARY);
 * </pre>
 */
public final class CategoryFixtures {

  // CategoryDomain fixtures (legacy - maintained for backward compatibility)
  public static final CategoryDomain OTHER_INCOME =
      CategoryDomain.withCategoryType(IncomeCategory.OTHER);

  public static final CategoryDomain PERSONAL_EXPENSE =
      CategoryDomain.withCategoryType(ExpenseCategory.PERSONAL);

  // CategoryDomain fixtures - Income categories
  public static final CategoryDomain SALARY_INCOME =
      CategoryDomain.withCategoryType(IncomeCategory.SALARY);

  public static final CategoryDomain INITIAL_BALANCE_INCOME =
      CategoryDomain.withCategoryType(IncomeCategory.INITIAL_BALANCE);

  public static final CategoryDomain DEPOSIT_INCOME =
      CategoryDomain.withCategoryType(IncomeCategory.DEPOSIT);

  // CategoryDomain fixtures - Expense categories
  public static final CategoryDomain SOCIAL_SECURITY_EXPENSE =
      CategoryDomain.withCategoryType(ExpenseCategory.SOCIAL_SECURITY);

  public static final CategoryDomain PUBLIC_SERVICES_EXPENSE =
      CategoryDomain.withCategoryType(ExpenseCategory.PUBLIC_SERVICES);

  // MovementCategoryVO fixtures - Income categories
  public static final MovementCategoryVO SALARY =
      MovementCategoryVO.withType(IncomeCategory.SALARY);

  public static final MovementCategoryVO INITIAL_BALANCE =
      MovementCategoryVO.withType(IncomeCategory.INITIAL_BALANCE);

  public static final MovementCategoryVO DEPOSIT =
      MovementCategoryVO.withType(IncomeCategory.DEPOSIT);

  public static final MovementCategoryVO OTHER_INCOME_MOVEMENT =
      MovementCategoryVO.withType(IncomeCategory.OTHER);

  public static final MovementCategoryVO TRANSFER_INCOME =
      MovementCategoryVO.withType(IncomeCategory.TRANSFER);

  public static final MovementCategoryVO INVESTMENT_INCOME =
      MovementCategoryVO.withType(IncomeCategory.INVESTMENT);

  // MovementCategoryVO fixtures - Expense categories
  public static final MovementCategoryVO PERSONAL =
      MovementCategoryVO.withType(ExpenseCategory.PERSONAL);

  public static final MovementCategoryVO PUBLIC_SERVICES =
      MovementCategoryVO.withType(ExpenseCategory.PUBLIC_SERVICES);

  public static final MovementCategoryVO SOCIAL_SECURITY =
      MovementCategoryVO.withType(ExpenseCategory.SOCIAL_SECURITY);

  public static final MovementCategoryVO TRANSFER_EXPENSE =
      MovementCategoryVO.withType(ExpenseCategory.TRANSFER);

  private CategoryFixtures() {
    throw new AssertionError("Utility class - do not instantiate");
  }
}
