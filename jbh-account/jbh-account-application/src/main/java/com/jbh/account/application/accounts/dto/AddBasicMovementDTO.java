package com.jbh.account.application.accounts.dto;

import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;

public record AddBasicMovementDTO(
    AccountDTO account, AccountMonthlyBalanceDomain monthlyBalance, MovementDTO movement) {}
