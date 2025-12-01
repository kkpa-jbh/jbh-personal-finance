package com.jbh.account.domain.vo;

import java.util.List;

@SuppressWarnings("PMD.LongVariable")
public enum ProductType {
  SAVINGS,
  REAL_ESTATE_INVESTMENT,
  LOAN,
  CREDIT_CARD,
  INVESTMENT,
  CDT, // Certificate of Deposit
  ;

  private static final List<ProductType> ADDING_MOVEMENTS_PRODUCTS_ALLOWED =
      List.of(SAVINGS, CREDIT_CARD, INVESTMENT, CDT);

  public boolean productTypeShouldUpdateMonthlyBalance() {
    return this == SAVINGS || this == CREDIT_CARD || this == INVESTMENT;
  }

  public List<ProductType> addingMovementsProductsAllowed() {
    return ADDING_MOVEMENTS_PRODUCTS_ALLOWED;
  }
}
