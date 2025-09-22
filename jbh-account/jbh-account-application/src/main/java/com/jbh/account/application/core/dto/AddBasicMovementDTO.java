package com.jbh.account.application.core.dto;

public record AddBasicMovementDTO(
    AccountDTO account, AccountMonthlyBalanceDTO monthlyBalance, MovementDTO movement) {}
