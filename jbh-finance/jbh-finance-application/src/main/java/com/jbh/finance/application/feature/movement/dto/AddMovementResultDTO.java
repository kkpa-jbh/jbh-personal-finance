package com.jbh.finance.application.feature.movement.dto;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;

public record AddMovementResultDTO(
    ProductDTO productDTO, MovementDTO movement, MonthlyBalanceDTO monthlyBalance) {}
