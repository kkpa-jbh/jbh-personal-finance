package com.jbh.account.application.core.dto;

public record AddBasicMovementDTO(
    AccountDTO account, MonthlyBalanceDTO monthlyBalance, MovementDTO movement) {}
