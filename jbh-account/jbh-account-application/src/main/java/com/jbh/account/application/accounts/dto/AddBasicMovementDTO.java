package com.jbh.account.application.accounts.dto;

import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.vo.AccountDomainDTO;

public record AddBasicMovementDTO(AccountDomainDTO account,
                                  AccountMonthlyBalanceDomain monthlyBalance,
                                  AccountMovementDomain movement) {

}
