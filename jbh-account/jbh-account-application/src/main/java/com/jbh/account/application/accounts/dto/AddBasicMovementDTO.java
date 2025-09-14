package com.jbh.account.application.accounts.dto;

import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountMovementDTO;

public record AddBasicMovementDTO(
    AccountDomainDTO account,
    AccountMonthlyBalanceDomain monthlyBalance,
    AccountMovementDTO movement) {}
