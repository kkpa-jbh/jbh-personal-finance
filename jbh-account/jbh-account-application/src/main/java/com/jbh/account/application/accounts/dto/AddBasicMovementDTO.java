package com.jbh.account.application.accounts.dto;

public record AddBasicMovementDTO(
    AccountDTO account, AccountMonthlyBalanceDTO monthlyBalance, MovementDTO movement) {}
