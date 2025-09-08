package com.jbh.account.application.accounts.dto;

import com.jbh.account.domain.accounts.AccountDomain;
import com.jbh.account.domain.accounts.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.movements.AccountMovementDomain;

public record AddBasicMovementDTO(AccountDomain account,
                                  AccountMonthlyBalanceDomain monthlyBalance,
                                  AccountMovementDomain movement) {

}
