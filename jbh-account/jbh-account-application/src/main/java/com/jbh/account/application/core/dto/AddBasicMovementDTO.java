package com.jbh.account.application.core.dto;

public record AddBasicMovementDTO(
    AccountDTO account, MonthlyBalanceDTO monthlyBalance, MovementDTO movement) {

  public AddBasicMovementDTO(final AccountDTO account, final MovementDTO movement) {
    this(account, null, movement);
  }
}
