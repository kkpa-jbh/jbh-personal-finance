package com.jbh.account_app.accounts.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddMovementWithDateAmount(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot) {

  public AddMovementWithDateAmount(LocalDate entryDate, BigDecimal totalAmount) {
    this(entryDate, totalAmount, null);
  }

  public void validate() {
    if (entryDate == null) {
      throw new IllegalArgumentException("Entry date cannot be null");
    }
    if (totalAmount == null && balanceSnapshot == null) {
      throw new IllegalArgumentException("There is not any amount to add");
    }
  }
}
