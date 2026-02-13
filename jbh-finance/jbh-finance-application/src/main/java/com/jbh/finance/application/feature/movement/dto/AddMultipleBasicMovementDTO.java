package com.jbh.finance.application.feature.movement.dto;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import java.util.List;

public record AddMultipleBasicMovementDTO(
    ProductDTO productDTO, List<MonthlyBalanceDTO> monthlyBalances) {}
