package com.jbh.finance.domain.movement.vo;

import com.jbh.commons.util.JbhStringUtils;

/**
 * CHATGPT
 *
 * <p>✅ Income Categories
 *
 * <p>Salary & Wages
 *
 * <p>Main Salary
 *
 * <p>Overtime Pay
 *
 * <p>Bonuses
 *
 * <p>Commissions
 *
 * <p>Business / Side Hustle
 *
 * <p>Freelance Income
 *
 * <p>Consulting
 *
 * <p>Small Business Revenue
 *
 * <p>Investment Income
 *
 * <p>Dividends
 *
 * <p>Interest (Savings Accounts, CDs)
 *
 * <p>Capital Gains
 *
 * <p>Rental Income
 *
 * <p>Property Rent
 *
 * <p>Airbnb / Short-Term Rental
 *
 * <p>Other Income
 *
 * <p>Gifts Received
 *
 * <p>Tax Refunds
 *
 * <p>Government Benefits (Unemployment, Social Security)
 *
 * <p>Reimbursements (Work Expenses, Health Insurance)
 */
public enum IncomeCategory implements CategoryType {
  TRANSFER("Transfer", "Transferencia"),
  SALARY("Salary", "Salario"),
  DIVIDENDS("Dividends", "Dividendos"),
  FREELANCE("Freelance", "Freelance"),
  INVESTMENT("Investment", "Inversión"),
  RENTAL("Rental", "Renta"),
  GIFT("Gift", "Regalo"),
  OTHER("Other", "Otro"),
  INITIAL_BALANCE("Initial Balance", "Saldo Inicial"),
  DEPOSIT("Deposit", "Depósito");

  private final String translationsKey;

  IncomeCategory(final String englishTranslation, final String spanishTranslation) {
    this.translationsKey = JbhStringUtils.buildJsonMessage(englishTranslation, spanishTranslation);
  }

  public static CategoryType findByName(final String categoryName) {
    return IncomeCategory.valueOf(categoryName);
  }

  @Override
  public CategorySource getSource() {
    return CategorySource.INCOME;
  }

  @Override
  public String getTypeName() {
    return this.name();
  }

  @Override
  public String getTranslationsKey() {
    return translationsKey;
  }

  @Override
  public String toString() {
    return "IncomeCategory{" + this.name() + "}";
  }
}
