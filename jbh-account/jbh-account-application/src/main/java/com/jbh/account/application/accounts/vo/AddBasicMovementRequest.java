package com.jbh.account.application.accounts.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object for adding a movement with date and amount.
 *
 * @param entryDate       Date of the movement
 * @param totalAmount     Positive for deposit, negative for withdrawal
 * @param balanceSnapshot Current balance after the movement (optional)
 */
public record AddBasicMovementRequest(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot) {

  public AddBasicMovementRequest(LocalDate entryDate, BigDecimal totalAmount) {
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
