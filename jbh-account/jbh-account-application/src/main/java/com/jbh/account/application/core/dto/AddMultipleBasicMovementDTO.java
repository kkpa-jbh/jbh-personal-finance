package com.jbh.account.application.core.dto;

import java.util.List;

public record AddMultipleBasicMovementDTO(
    ProductDTO account, List<MonthlyBalanceDTO> monthlyBalances) {}
