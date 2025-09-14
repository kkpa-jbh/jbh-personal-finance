package com.jbh.account.application.accounts.dto;

import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.util.List;

public record AddMultipleBasicMovementDTO(
    AccountDomainDTO account, List<AccountMonthlyBalanceDTO> monthlyBalances) {}
