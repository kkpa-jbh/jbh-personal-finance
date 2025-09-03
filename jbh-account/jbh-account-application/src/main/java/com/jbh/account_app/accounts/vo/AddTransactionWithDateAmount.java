package com.jbh.account_app.accounts.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AddTransactionWithDateAmount(
    UUID userId,
    LocalDate txnDate,
    BigDecimal totalAmount) {

  public void validate() {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    if (txnDate == null) {
      throw new IllegalArgumentException("Transaction date cannot be null");
    }
    if (totalAmount == null) {
      throw new IllegalArgumentException("Total amount cannot be null");
    }
  }
}
