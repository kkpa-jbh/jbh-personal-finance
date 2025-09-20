package com.jbh.account.application.accounts.dto;

import java.util.List;

public record AddMultipleBasicMovementDTO(
    AccountDTO account, List<AccountMonthlyBalanceDTO> monthlyBalances) {}
