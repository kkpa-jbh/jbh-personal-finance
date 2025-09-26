package com.jbh.account.application.core.dto;

public record AddBasicMovementDTO(
    AccountDTO account, MovementDTO movement, MonthlyBalanceDTO monthlyBalance) {}
