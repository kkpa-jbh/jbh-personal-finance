package com.jbh.account.application.core.dto;

import java.util.List;

public record AddMultipleBasicMovementDTO(
    AccountDTO account, List<MonthlyBalanceDTO> monthlyBalances) {}
