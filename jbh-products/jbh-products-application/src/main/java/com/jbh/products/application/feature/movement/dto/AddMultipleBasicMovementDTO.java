package com.jbh.products.application.feature.movement.dto;

import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import java.util.List;

public record AddMultipleBasicMovementDTO(
    ProductDTO account, List<MonthlyBalanceDTO> monthlyBalances) {}
