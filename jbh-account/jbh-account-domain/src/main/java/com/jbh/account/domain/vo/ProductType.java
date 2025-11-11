package com.jbh.account.domain.vo;

public enum ProductType {
  SAVINGS,
  LOAN,
  CREDIT_CARD,
  INVESTMENT,
  CDT, // Certificate of Deposit
  ;

  public boolean productTypeShouldUpdateMonthlyBalance() {
    return this == SAVINGS || this == CREDIT_CARD || this == INVESTMENT;
  }
}
