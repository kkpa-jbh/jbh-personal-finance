package com.jbh.products.application.core.dto;

import java.util.List;

public record AddMultipleBasicMovementDTO(
    ProductDTO account, List<MonthlyBalanceDTO> monthlyBalances) {}
