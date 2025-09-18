package com.jbh.account.domain.vo;

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
  SALARY,
  FREELANCE,
  INVESTMENT,
  RENTAL,
  GIFT,
  OTHER;

  @Override
  public CategorySource getSource() {
    return CategorySource.INCOME;
  }
}
