package com.jbh.account.application.core.dto;

public record AddBasicMovementDTO(
    ProductDTO account, MovementDTO movement, MonthlyBalanceDTO monthlyBalance) {}
