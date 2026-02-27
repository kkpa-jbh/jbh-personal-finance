package com.jbh.finance.domain.category;

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
*
* <p>TRANSFER("Transfer", "Transferencia"), SALARY("Salary", "Salario"), DIVIDENDS("Dividends",
* "Dividendos"), FREELANCE("Freelance", "Freelance"), INVESTMENT("Investment", "Inversión"),
* RENTAL("Rental", "Renta"), GIFT("Gift", "Regalo"), OTHER("Other", "Otro"),
* INITIAL_BALANCE("Initial Balance", "Saldo Inicial"), DEPOSIT("Deposit", "Depósito");
  */
  @SuppressWarnings("PMD.UnusedPrivateField")
  public enum IncomeCategory {
  TRANSFER,
  SALARY,
  DIVIDENDS,
  FREELANCE,
  RENTAL,
  GIFT,
  OTHER,
  INITIAL_BALANCE,
  DEPOSIT;

@Override
public String toString() {
return "IncomeCategory{" + this.name() + "}";
}
}
