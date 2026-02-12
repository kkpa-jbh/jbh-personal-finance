package com.jbh.finance.application.feature.movement.dto;

import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import java.util.List;

public record AddMultipleBasicMovementDTO(
    ProductDTO account, List<MonthlyBalanceDTO> monthlyBalances) {}
