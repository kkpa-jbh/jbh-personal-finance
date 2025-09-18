package com.jbh.account.application.accounts.dto;

import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.util.List;

public record AddMultipleBasicMovementDTO(
    AccountDTO account, List<AccountMonthlyBalanceDTO> monthlyBalances) {}
