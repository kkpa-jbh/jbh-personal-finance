package com.jbh.account.domain.vo;

public enum ProductType {
  SAVINGS,
  CREDIT_CARD,
  INVESTMENT,
  CDT, // Certificate of Deposit
  ;

  public boolean productTypeShouldUpdateMonthlyBalance() {
    return this == SAVINGS || this == CREDIT_CARD || this == INVESTMENT;
  }
}
