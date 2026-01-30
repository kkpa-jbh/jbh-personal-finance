package com.jbh.products.application.core.dto;

public record AddBasicMovementDTO(
    ProductDTO account, MovementDTO movement, MonthlyBalanceDTO monthlyBalance) {}
