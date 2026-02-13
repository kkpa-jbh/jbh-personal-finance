package com.jbh.finance.application.feature.movement.dto;

import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;

public record AddBasicMovementDTO(
    ProductDTO productDTO, MovementDTO movement, MonthlyBalanceDTO monthlyBalance) {}
