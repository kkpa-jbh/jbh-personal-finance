package com.jbh.products.application.feature.movement.dto;

import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.monthlybalance.dto.MonthlyBalanceDTO;

public record AddBasicMovementDTO(
    ProductDTO account, MovementDTO movement, MonthlyBalanceDTO monthlyBalance) {}
